# 'Gotcha' 스타일 사물 누끼 도감 앱 개발 PRD (Android)

## 1. 프로젝트 개요 (Overview)
*   **프로젝트명:** Gotcha Lite (Android 온디바이스 누끼 도감)
*   **개발 목표:** 외부 네트워크 통신이나 유료 API Key 없이, 안드로이드 전용 온디바이스 AI SDK를 활용하여 사물의 배경을 지우고(누끼 추출), FusedLocationProvider로 위치 값을 받아 세련된 카드 레이아웃에 배치하여 로컬 저장소에 수집하는 네이티브 앱 구현.
*   **타겟 환경:** Jetpack Compose, Android SDK 34 (minSdkVersion: 24)

## 2. 제품 핵심 기능 요건 (Product Features)
### F-1. 무권한 미디어 수집 (Photo Picker)
*   Android 11 이상 지원 최신 `PickVisualMedia` API를 적용해 복잡한 파일 읽기 권한 없이도 안전하게 사용자의 사물 이미지를 확보합니다.

### F-2. 온디바이스 피사체 분리 (ML Kit Subject Segmentation)
*   `play-services-mlkit-subject-segmentation` 라이브러리를 이용하여 이미지 내 주요 사물을 구분해 내고, 배경 영역을 투명하게 처리한 `Bitmap` 객체를 획득합니다.

### F-3. 위치 정보(GPS) 및 수집 메타데이터 생성
*   `FusedLocationProviderClient`를 사용해 포그라운드 대략적 위치(ACCESS_COARSE_LOCATION) 좌표를 추출합니다.
*   가져온 위/경도를 바탕으로 로컬 가상 맵핑 코드를 태워 희귀도 뱃지 데이터를 생성합니다.

### F-4. Jetpack Compose 기반 도감 카드 UI & 로컬 저장
*   획득한 누끼 비트맵을 카드 형태 레이아웃에 로딩합니다.
*   가공된 이미지(PNG)를 앱 전용 내부 저장 공간(`context.filesDir`)에 저장하고 앱 시작 시 해당 폴더의 파일명 리스트를 로드하여 그리드 뷰를 그립니다.

## 3. 기술 스택 및 종속성 (build.gradle.kts)
```kotlin
dependencies {
    implementation("com.google.android.gms:play-services-location:21.2.0") // GPS 위치 수집용
    implementation("com.google.mlkit:subject-segmentation:16.0.0-beta1")  // 구글 순수 오프라인 누끼 SDK
    implementation("io.coil-kt:coil-compose:2.6.0")                         // 비트맵 뷰어 컴포넌트
}
```

## 4. 90분 핸즈온 실습 타임라인
*   **00분 ~ 15분 (환경 설정):** Empty Compose Activity 생성, build.gradle에 플레이 서비스 위치 및 ML Kit 탑재, Manifest에 권한 선언.
*   **15분 ~ 35분 (Photo Picker 및 GPS):** 최신 Photo Picker 연결 및 위치 서비스 기본 바인딩 완료.
*   **35분 ~ 55분 (누끼 처리기 구현):** `SubjectSegmentation` API를 연동하여 결과 `Bitmap`을 화면에 렌더링.
*   **55분 ~ 75분 (내부 저장소 이미지 쓰기):** 생성된 누끼 비트맵을 PNG 파일로 압축해 `context.filesDir` 폴더에 쓰고 그리드 도감 형태로 목록 뷰 연동.
*   **75분 ~ 90분 (실 기기 테스트):** 에뮬레이터 위치값 가상 이동 및 오프라인 촬영 테스트 진행.

## 5. 실패 방지 핵심 개발 가이드 (Android Code)

### A. AndroidManifest.xml 설정 (`AndroidManifest.xml`)
안전한 GPS 수집 권한 및 AI 모델 백그라운드 자동 다운로드를 활성화합니다.
```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    <uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />

    <application ...>
        <meta-data
            android:name="com.google.mlkit.vision.DEPENDENCIES"
            android:value="subject_segment" /> <!-- 모델 자동 다운로드 지정 -->
    </application>
</manifest>
```

### B. 누끼 추출 핵심 Kotlin 비질니스 로직
```kotlin
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.segmentation.subject.SubjectSegmentation
import com.google.mlkit.vision.segmentation.subject.SubjectSegmenterOptions
import kotlinx.coroutines.tasks.await
import java.io.File
import java.io.FileOutputStream

class NukiService(context: Context) {
    // 1. 누끼 추출 옵션 빌더 선언
    private val options = SubjectSegmenterOptions.Builder()
        .enableForegroundBitmap() // 누끼 비트맵 결과 가져오기 활성화
        .build()
        
    private val segmenter = SubjectSegmentation.getClient(options)

    // 2. 비동기 이미지 누끼 처리 함수 (Coroutines 사용)
    suspend fun processSubjectImage(context: Context, imageUri: Uri): Bitmap? {
        return try {
            val inputImage = InputImage.fromFilePath(context, imageUri)
            val result = segmenter.process(inputImage).await()
            result.foregroundBitmap // 투명 배경 비트맵 직접 획득
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // 3. 내부 저장소에 파일 물리 저장
    fun saveNukiToFile(context: Context, bitmap: Bitmap): String {
        val fileName = "gotcha_${System.currentTimeMillis()}.png"
        val file = File(context.filesDir, fileName)
        val outputStream = FileOutputStream(file)
        
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
        outputStream.flush()
        outputStream.close()
        
        return file.absolutePath
    }
}
```
