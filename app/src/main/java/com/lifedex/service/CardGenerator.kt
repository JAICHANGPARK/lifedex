package com.lifedex.service

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CardGenerator {

    private const val TAG = "CardGenerator"

    enum class CardElementType(
        val typeName: String,
        val colorHex: String,
        val bgStartHex: String,
        val bgEndHex: String,
        val symbol: String,
        val weaknessSymbol: String,
        val resistanceSymbol: String
    ) {
        FIRE("FIRE", "#EF4444", "#FCA5A5", "#7F1D1D", "🔥", "💧", "⚡"),
        WATER("WATER", "#3B82F6", "#93C5FD", "#1E3A8A", "💧", "🍃", "🔥"),
        GRASS("GRASS", "#10B981", "#6EE7B7", "#064E3B", "🍃", "🔥", "💧"),
        LIGHTNING("LIGHTNING", "#FBBF24", "#FDE047", "#78350F", "⚡", "🔮", "⚪"),
        PSYCHIC("PSYCHIC", "#8B5CF6", "#C084FC", "#4C1D95", "🔮", "🍃", "⚪"),
        NORMAL("NORMAL", "#94A3B8", "#E2E8F0", "#334155", "⚪", "🔥", "🔮")
    }

    private fun getAttack1Name(type: CardElementType, seed: Int): String {
        val names = when (type) {
            CardElementType.FIRE -> listOf("불꽃 펀치", "불씨날리기", "화염 세례")
            CardElementType.WATER -> listOf("물대포", "거품광선", "아쿠아 링")
            CardElementType.GRASS -> listOf("잎날 가르기", "덩굴 채찍", "가루뿌리기")
            CardElementType.LIGHTNING -> listOf("전기 쇼크", "스파크", "전자포")
            CardElementType.PSYCHIC -> listOf("염동력", "환상빔", "최면술")
            CardElementType.NORMAL -> listOf("몸통 박치기", "할퀴기", "웅크리기")
        }
        return names[Math.abs(seed) % names.size]
    }

    private fun getAttack2Name(type: CardElementType, seed: Int): String {
        val names = when (type) {
            CardElementType.FIRE -> listOf("홍련의 바람", "플레어 드라이브", "화염 방사")
            CardElementType.WATER -> listOf("하이드로 펌프", "파도타기", "아쿠아 댐")
            CardElementType.GRASS -> listOf("솔라 빔", "리프 스톰", "기가 드레인")
            CardElementType.LIGHTNING -> listOf("번개 치기", "볼트 태클", "백만 볼트")
            CardElementType.PSYCHIC -> listOf("사이코키네시스", "섀도볼", "미래 예지")
            CardElementType.NORMAL -> listOf("파괴 광선", "메가톤 펀치", "기가 임팩트")
        }
        return names[Math.abs(seed) % names.size]
    }

    private fun getAttack1Desc(type: CardElementType, seed: Int): String {
        val descs = when (type) {
            CardElementType.FIRE -> listOf("상대에게 화상을 입힌다.", "동전을 던져 앞면이 나오면 10데미지를 추가한다.")
            CardElementType.WATER -> listOf("상대의 후퇴 에너지를 1개 늘린다.", "자신의 에너지를 1개 회복한다.")
            CardElementType.GRASS -> listOf("상대를 독 상태로 만든다.", "상대의 특성을 봉인한다.")
            CardElementType.LIGHTNING -> listOf("동전을 던져 앞면이 나오면 상대를 마비 상태로 만든다.", "상대 벤치에게도 10데미지를 준다.")
            CardElementType.PSYCHIC -> listOf("상대를 혼란 상태로 만든다.", "상대의 패를 1장 덱으로 돌린다.")
            CardElementType.NORMAL -> listOf("자신의 다음 차례에 받는 데미지를 -20한다.", "이 크리처에게도 10데미지를 준다.")
        }
        return descs[Math.abs(seed) % descs.size]
    }

    private fun getAttack2Desc(type: CardElementType, seed: Int): String {
        val descs = when (type) {
            CardElementType.FIRE -> listOf("이 크리처에게서 불꽃 에너지를 2개 트래시한다.", "상대 크리처를 완전히 태워버린다.")
            CardElementType.WATER -> listOf("상대의 모든 에너지를 씻어낸다.", "세찬 수압으로 폭발적인 타격을 입힌다.")
            CardElementType.GRASS -> listOf("이 크리처의 HP를 30 회복한다.", "태양 에너지를 충전하여 강력한 빔을 쏜다.")
            CardElementType.LIGHTNING -> listOf("이 크리처도 30데미지를 받는다.", "번개 같은 몸놀림으로 치명타를 가한다.")
            CardElementType.PSYCHIC -> listOf("상대 배틀 크리처의 에너지를 1개 트래시한다.", "기묘한 염력을 발산하여 혼란을 극대화한다.")
            CardElementType.NORMAL -> listOf("다음 차례에 이 크리처는 기술을 사용할 수 없다.", "엄청난 질량으로 상대를 들이받는다.")
        }
        return descs[Math.abs(seed) % descs.size]
    }

    suspend fun generateCardImage(
        stickerBitmap: Bitmap,
        objectLabel: String,
        rarity: String,
        level: Int
    ): Bitmap? = withContext(Dispatchers.Default) {
        try {
            val cardWidth = 800
            val cardHeight = 1120 // 5:7 ratio (standard card)
            
            val resultBitmap = Bitmap.createBitmap(cardWidth, cardHeight, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(resultBitmap)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG)
            
            val isEx = (rarity.uppercase() == "LEGENDARY" || rarity.uppercase() == "EPIC")
            
            // Determine type by hash
            val seed = objectLabel.hashCode()
            val typeIndex = Math.abs(seed) % CardElementType.values().size
            val type = CardElementType.values()[typeIndex]
            
            // Procedural attacks
            val att1Name = getAttack1Name(type, seed)
            val att2Name = getAttack2Name(type, seed)
            val att1Desc = getAttack1Desc(type, seed)
            val att2Desc = getAttack2Desc(type, seed)
            val att1Dmg = (30 + Math.abs(seed % 4) * 10).toString()
            val att2Dmg = (100 + Math.abs(seed % 6) * 20).toString()
            
            val cornerRadius = 48f
            val cardRect = RectF(0f, 0f, cardWidth.toFloat(), cardHeight.toFloat())

            // 1. Draw Card Background
            if (isEx) {
                // EX cards have a full-art holographic/cosmic gradient
                val exBgShader = RadialGradient(
                    cardWidth / 2f, cardHeight / 2f, cardWidth.toFloat(),
                    Color.parseColor(type.bgStartHex), Color.parseColor(type.bgEndHex),
                    Shader.TileMode.CLAMP
                )
                paint.shader = exBgShader
                paint.style = Paint.Style.FILL
                canvas.drawRoundRect(cardRect, cornerRadius, cornerRadius, paint)
                
                // Draw EX rainbow metallic border
                paint.shader = LinearGradient(
                    0f, 0f, cardWidth.toFloat(), cardHeight.toFloat(),
                    intArrayOf(
                        Color.parseColor("#FFD700"), // Gold
                        Color.parseColor("#EC4899"), // Fuchsia
                        Color.parseColor("#3B82F6"), // Blue
                        Color.parseColor("#10B981"), // Emerald
                        Color.parseColor("#FFD700")  // Gold
                    ),
                    floatArrayOf(0f, 0.25f, 0.5f, 0.75f, 1f),
                    Shader.TileMode.CLAMP
                )
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 24f
                canvas.drawRoundRect(cardRect, cornerRadius, cornerRadius, paint)
            } else {
                // Standard cards have a yellow/gold border and a cleaner inner card
                paint.shader = null
                paint.color = Color.parseColor("#FBBF24") // Pokémon Gold Border
                paint.style = Paint.Style.FILL
                canvas.drawRoundRect(cardRect, cornerRadius, cornerRadius, paint)
                
                val innerRect = RectF(24f, 24f, cardWidth - 24f, cardHeight - 24f)
                paint.color = Color.parseColor("#F1F5F9") // Light gray-blue inner surface
                canvas.drawRoundRect(innerRect, 32f, 32f, paint)
            }
            
            // 2. Draw Illustration Box
            val imageBoxLeft = 50f
            val imageBoxTop = 150f
            val imageBoxRight = cardWidth - 50f
            val imageBoxBottom = 590f
            val imageRect = RectF(imageBoxLeft, imageBoxTop, imageBoxRight, imageBoxBottom)

            if (!isEx) {
                // Draw a beautiful themed frame gradient for standard cards
                val boxGradient = RadialGradient(
                    cardWidth / 2f, (imageBoxTop + imageBoxBottom) / 2f, 320f,
                    Color.parseColor(type.bgStartHex), Color.parseColor(type.bgEndHex),
                    Shader.TileMode.CLAMP
                )
                paint.shader = boxGradient
                paint.style = Paint.Style.FILL
                canvas.drawRoundRect(imageRect, 20f, 20f, paint)
                
                // Silver inner border
                paint.shader = null
                paint.color = Color.parseColor("#CBD5E1")
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 8f
                canvas.drawRoundRect(imageRect, 20f, 20f, paint)
            } else {
                // Draw a subtle translucent overlay behind the sticker to make it pop
                paint.shader = null
                paint.color = Color.argb(40, 255, 255, 255)
                paint.style = Paint.Style.FILL
                canvas.drawRoundRect(imageRect, 24f, 24f, paint)
            }

            // 3. Draw the Sticker Bitmap (overlap for 3D effect in EX cards)
            paint.shader = null
            paint.style = Paint.Style.FILL
            val padding = if (isEx) 0f else 32f
            val scaleX = (imageRect.width() - padding) / stickerBitmap.width
            val scaleY = (imageRect.height() - padding) / stickerBitmap.height
            val scale = minOf(scaleX, scaleY) * (if (isEx) 1.15f else 1.0f) // Pop out for EX
            
            val destWidth = stickerBitmap.width * scale
            val destHeight = stickerBitmap.height * scale
            val left = imageRect.centerX() - (destWidth / 2f)
            val top = imageRect.centerY() - (destHeight / 2f) - (if (isEx) 20f else 0f)
            val destRect = RectF(left, top, left + destWidth, top + destHeight)
            
            canvas.drawBitmap(stickerBitmap, null, destRect, null)
            
            // 4. Header Section (drawn on top of the image in EX for authenticity)
            paint.textAlign = Paint.Align.LEFT
            paint.color = if (isEx) Color.WHITE else Color.parseColor("#1E293B")
            paint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            paint.textSize = 46f
            
            val nameText = objectLabel
            val nameWidth = paint.measureText(nameText)
            canvas.drawText(nameText, 70f, 105f, paint)
            
            if (isEx) {
                // Draw "ex" tag in red italic next to name
                paint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD_ITALIC)
                paint.color = Color.parseColor("#EF4444") // ex red tag
                paint.textSize = 34f
                canvas.drawText(" ex", 70f + nameWidth, 105f, paint)
            }
            
            // HP Value
            val hpValue = (level * 2).coerceAtLeast(10) + 70
            paint.color = Color.parseColor("#EF4444")
            paint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            paint.textSize = 42f
            paint.textAlign = Paint.Align.RIGHT
            canvas.drawText("HP $hpValue", 660f, 105f, paint)
            
            // Element Symbol next to HP
            paint.color = Color.parseColor(type.colorHex)
            paint.style = Paint.Style.FILL
            canvas.drawCircle(715f, 90f, 22f, paint)
            paint.color = Color.WHITE
            paint.textAlign = Paint.Align.CENTER
            paint.textSize = 28f
            canvas.drawText(type.symbol, 715f, 100f, paint)
            
            // 5. Divider
            paint.color = if (isEx) Color.argb(120, 255, 255, 255) else Color.parseColor("#CBD5E1")
            paint.strokeWidth = 4f
            canvas.drawLine(50f, 610f, cardWidth - 50f, 610f, paint)
            
            // 6. Attack 1
            val atk1Y = 680f
            // Energy Cost circles
            paint.color = Color.parseColor(type.colorHex)
            canvas.drawCircle(80f, atk1Y - 10f, 18f, paint)
            paint.color = Color.WHITE
            paint.textSize = 24f
            paint.textAlign = Paint.Align.CENTER
            canvas.drawText(type.symbol, 80f, atk1Y - 1f, paint)
            
            // If it's a stronger card, maybe 2 energy costs
            if (level > 30) {
                paint.color = Color.parseColor("#94A3B8") // colorless
                canvas.drawCircle(120f, atk1Y - 10f, 18f, paint)
                paint.color = Color.WHITE
                canvas.drawText("⚪", 120f, atk1Y - 1f, paint)
            }
            
            // Attack 1 Name
            paint.color = if (isEx) Color.WHITE else Color.parseColor("#1E293B")
            paint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            paint.textSize = 34f
            paint.textAlign = Paint.Align.LEFT
            val nameAtk1X = if (level > 30) 160f else 120f
            canvas.drawText(att1Name, nameAtk1X, atk1Y, paint)
            
            // Damage 1
            paint.textAlign = Paint.Align.RIGHT
            canvas.drawText(att1Dmg, cardWidth - 70f, atk1Y, paint)
            
            // Description 1
            paint.color = if (isEx) Color.parseColor("#E2E8F0") else Color.parseColor("#475569")
            paint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            paint.textSize = 20f
            paint.textAlign = Paint.Align.LEFT
            canvas.drawText(att1Desc, 80f, atk1Y + 36f, paint)
            
            // 7. Attack 2
            val atk2Y = 810f
            // Energy Cost circles (3 circles: 2 types, 1 colorless)
            paint.color = Color.parseColor(type.colorHex)
            canvas.drawCircle(80f, atk2Y - 10f, 18f, paint)
            canvas.drawCircle(120f, atk2Y - 10f, 18f, paint)
            paint.color = Color.parseColor("#94A3B8")
            canvas.drawCircle(160f, atk2Y - 10f, 18f, paint)
            
            paint.color = Color.WHITE
            paint.textAlign = Paint.Align.CENTER
            canvas.drawText(type.symbol, 80f, atk2Y - 1f, paint)
            canvas.drawText(type.symbol, 120f, atk2Y - 1f, paint)
            canvas.drawText("⚪", 160f, atk2Y - 1f, paint)
            
            // Attack 2 Name
            paint.color = if (isEx) Color.WHITE else Color.parseColor("#1E293B")
            paint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            paint.textSize = 34f
            paint.textAlign = Paint.Align.LEFT
            canvas.drawText(att2Name, 200f, atk2Y, paint)
            
            // Damage 2
            paint.textAlign = Paint.Align.RIGHT
            canvas.drawText(att2Dmg, cardWidth - 70f, atk2Y, paint)
            
            // Description 2
            paint.color = if (isEx) Color.parseColor("#E2E8F0") else Color.parseColor("#475569")
            paint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            paint.textSize = 20f
            paint.textAlign = Paint.Align.LEFT
            canvas.drawText(att2Desc, 80f, atk2Y + 36f, paint)
            
            // 8. Weakness, Resistance, Retreat Section
            val footerY = 940f
            paint.color = if (isEx) Color.argb(120, 255, 255, 255) else Color.parseColor("#CBD5E1")
            canvas.drawLine(50f, footerY - 30f, cardWidth - 50f, footerY - 30f, paint)
            
            // Weakness
            paint.color = if (isEx) Color.parseColor("#94A3B8") else Color.parseColor("#64748B")
            paint.textSize = 18f
            paint.textAlign = Paint.Align.LEFT
            canvas.drawText("약점", 80f, footerY, paint)
            
            // Weakness symbol
            paint.color = Color.parseColor("#EF4444")
            paint.textSize = 20f
            canvas.drawText("${type.weaknessSymbol} +20", 80f, footerY + 32f, paint)
            
            // Resistance
            paint.color = if (isEx) Color.parseColor("#94A3B8") else Color.parseColor("#64748B")
            paint.textSize = 18f
            canvas.drawText("저항력", 330f, footerY, paint)
            canvas.drawText("${type.resistanceSymbol} -30", 330f, footerY + 32f, paint)
            
            // Retreat
            canvas.drawText("후퇴", 580f, footerY, paint)
            canvas.drawText("⚪ ⚪", 580f, footerY + 32f, paint)
            
            // 9. ex Rule box / Standard Footer
            if (isEx) {
                // Draw standard ex rule box at the bottom
                val ruleRect = RectF(70f, 1020f, cardWidth - 70f, 1080f)
                
                // Gold Rule Box background
                paint.shader = LinearGradient(
                    ruleRect.left, ruleRect.top, ruleRect.right, ruleRect.bottom,
                    Color.parseColor("#FBBF24"), Color.parseColor("#F59E0B"), Shader.TileMode.CLAMP
                )
                paint.style = Paint.Style.FILL
                canvas.drawRoundRect(ruleRect, 12f, 12f, paint)
                
                paint.shader = null
                paint.style = Paint.Style.STROKE
                paint.color = Color.parseColor("#D97706")
                paint.strokeWidth = 2f
                canvas.drawRoundRect(ruleRect, 12f, 12f, paint)
                
                // Rule Text
                paint.style = Paint.Style.FILL
                paint.color = Color.BLACK
                paint.textAlign = Paint.Align.CENTER
                paint.textSize = 18f
                paint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                canvas.drawText("크리처 ex가 기절한 경우 상대는 2포인트 가져간다.", cardWidth / 2f, 1056f, paint)
            }
            
            // Copyrights & Illustrators
            paint.color = if (isEx) Color.parseColor("#94A3B8") else Color.parseColor("#64748B")
            paint.textSize = 16f
            paint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            paint.textAlign = Paint.Align.LEFT
            canvas.drawText("Illus. Gotcha AI Studio", 80f, 1000f, paint)
            
            paint.textAlign = Paint.Align.RIGHT
            val year = SimpleDateFormat("yyyy", Locale.US).format(Date())
            canvas.drawText("© $year LifeDex / Gotcha-Lite", cardWidth - 70f, 1000f, paint)
            
            // 10. Holo-foil diagonal shine lines (translucent stripes)
            paint.shader = null
            paint.color = Color.WHITE
            paint.alpha = 24 // super faint shine
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 35f
            canvas.drawLine(-100f, 200f, 700f, 1000f, paint)
            canvas.drawLine(100f, 0f, 900f, 800f, paint)
            canvas.drawLine(350f, -100f, 1150f, 700f, paint)
            
            Log.d(TAG, "Successfully generated high-fidelity Creature card image!")
            resultBitmap
        } catch (e: Exception) {
            Log.e(TAG, "High-fidelity Creature card image generation error: ${e.message}")
            null
        }
    }
}
