# 'Gotcha' 스타일 사물 누끼 도감 앱 개발 PRD (Flutter)

## 1. 프로젝트 개요 (Overview)
*   **프로젝트명:** Gotcha Lite (Flutter 온디바이스 누끼 도감)
*   **개발 목표:** 외부 API 통신이나 유료 인증키(API Key) 없이, 기기 자체 AI 모델을 사용해 사진 속 사물의 배경을 즉시 지우고(누끼 추출), 획득한 위치(GPS) 정보와 함께 크리처 도감 스타일의 카드로 수집 및 저장하는 크로스 플랫폼 앱 구현.
*   **타겟 환경:** Flutter SDK 3.19.x 이상, Android (minSdkVersion: 24, targetSdkVersion: 34)

## 2. 제품 핵심 기능 요건 (Product Features)
### F-1. 카메라 촬영 및 갤러리 연동
*   사용자는 앱 내 버튼을 통해 사진을 찍거나 갤러리에서 대상을 선택합니다.
*   인텐트 방식을 활용하여 카메라 및 권한 관련 예외 상황을 최소화합니다.

### F-2. 온디바이스 피사체 분리 (ML Kit Subject Segmentation)
*   구글의 100% 무료 온디바이스 ML Kit을 사용하여 이미지에서 전경 피사체만 분리(누끼 추출)합니다.
*   추출된 피사체는 투명 배경을 가진 PNG 이미지 포맷(Uint8List)으로 가공됩니다.

### F-3. 위치 정보(GPS) 및 가상 희귀도(Rarity) 수집
*   사진이 찍히는 시점의 실시간 위도(Latitude)와 경도(Longitude)를 취득합니다.
*   취득된 위경도의 소수점 연산을 활용하여 도감 내 희귀도(Common / Rare / Legendary)를 난수로 자동 판정해 재미 요소를 더합니다.

### F-4. 카드 뷰(Gotcha Style) UI 및 도감 저장
*   추출한 투명 PNG 누끼 이미지를 둥근 그림자 카드 중앙에 배치합니다.
*   카드에는 희귀도 뱃지, 도감 번호, 수집 좌표가 깔끔하게 노출됩니다.
*   로컬 저장소 폴더에 가공된 이미지 파일들을 저장하여 앱 재실행 시에도 데이터가 보존되도록 구현합니다.

## 3. 기술 스택 및 종속성 (pubspec.yaml)
```yaml
dependencies:
  flutter:
    sdk: flutter
  image_picker: ^1.1.2                            # 시스템 카메라/갤러리 인터페이스 호출
  geolocator: ^12.0.0                             # GPS 정보 수집용 (Play Services 연동)
  google_mlkit_subject_segmentation: ^0.0.3        # 오프라인 누끼따기 플러그인 (Android 지원)
  path_provider: ^2.1.3                           # 로컬 이미지 영구 저장을 위한 폴더 경로 획득
```

## 4. 90분 핸즈온 실습 타임라인
*   **00분 ~ 15분 (환경 설정):** 패키지 추가 및 AndroidManifest.xml에 자동 모델 다운로드 선언 추가.
*   **15분 ~ 35분 (이미지 획득 및 GPS):** 사진을 불러오고 `geolocator`로 위경도를 동시 확보하여 화면에 표기.
*   **35분 ~ 55분 (누끼 추출 구현):** `google_mlkit_subject_segmentation`을 활용해 투명 PNG 바이트 데이터 추출 기능 구현.
*   **55분 ~ 75분 (로컬 저장 및 도감 구현):** 변환 이미지를 로컬 디렉터리에 쓰고 앱 구동 시 해당 폴더의 파일 목록을 긁어와 GridView(도감 목록)로 렌더링.
*   **75분 ~ 90분 (테스트 및 트러블슈팅):** 주변의 사물을 직접 촬영하여 도감에 채우는 시연 진행.

## 5. 실패 방지 핵심 개발 가이드 (Flutter Code)

### A. Android 설정 (`android/app/src/main/AndroidManifest.xml`)
앱 설치 시 구글 플레이 서비스에서 누끼용 AI 모델을 자동으로 내려받도록 아래 설정을 추가합니다.
```xml
<application ...>
    <meta-data
        android:name="com.google.mlkit.vision.DEPENDENCIES"
        android:value="subject_segment" /> <!-- 설치 시 온디바이스 모델 다운로드 -->
</application>
```

### B. 누끼 추출 핵심 Dart 비즈니스 로직
```dart
import 'dart:io';
import 'dart:typed_data';
import 'package:google_mlkit_subject_segmentation/google_mlkit_subject_segmentation.dart';
import 'package:path_provider/path_provider.dart';

class PokedexService {
  final _segmenter = SubjectSegmenter(
    options: SubjectSegmenterOptions(
      enableForegroundBitmap: true, // 누끼딴 결과 비트맵 활성화
      enableForegroundConfidenceMask: false,
    ),
  );

  // 1. 누끼 추출 비즈니스 로직
  Future<Uint8List?> extractSubject(String imagePath) async {
    final inputImage = InputImage.fromFilePath(imagePath);
    try {
      final result = await _segmenter.processImage(inputImage);
      return result.foregroundBitmap; // 투명 배경 PNG 이미지 바이트 반환
    } catch (e) {
      print("누끼 연산 실패: $e");
      return null;
    }
  }

  // 2. 파일 영구 저장 처리 로직
  Future<String> saveNukiImage(Uint8List bytes) async {
    final directory = await getApplicationDocumentsDirectory();
    final fileName = "nuki_${DateTime.now().millisecondsSinceEpoch}.png";
    final file = File('${directory.path}/$fileName');
    await file.writeAsBytes(bytes);
    return file.path; // 추후 로딩을 위해 이미지 로컬 경로 반환
  }
}
```
