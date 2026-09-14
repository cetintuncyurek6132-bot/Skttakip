package com.example.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Modern, sade ve profesyonel açılış (Splash) animasyonu.
 * Toplam süre: ~1.4 saniye.
 */
@Composable
fun AppSplashScreen(
    onSplashFinished: () -> Unit
) {
    // Animasyon durumları - İlk karede doğrudan görünür başlar (kara ekran beklemesi sıfırlandı)
    val sktAlpha = remember { Animatable(0.2f) }
    val sktScale = remember { Animatable(0.92f) }
    val glowAlpha = remember { Animatable(0.3f) }

    val subtitleAlpha = remember { Animatable(0f) }
    val subtitleOffset = remember { Animatable(6f) }

    val exitAlpha = remember { Animatable(1f) }
    val exitScale = remember { Animatable(1f) }

    LaunchedEffect(Unit) {
        // Aşama 1: SKT harfleri ve arka turkuaz ışıltı hemen akıcı şekilde tam netliğe ulaşır (~300 ms)
        val sktAlphaJob = launch {
            sktAlpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = 300,
                    easing = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)
                )
            )
        }
        val sktScaleJob = launch {
            sktScale.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = 300,
                    easing = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)
                )
            )
        }
        val glowJob = launch {
            glowAlpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = 350,
                    easing = LinearEasing
                )
            )
        }

        // Aşama 2: "Takip & Stok" alt başlığı fade-in (~280 ms)
        delay(60)
        val subtitleAlphaJob = launch {
            subtitleAlpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = 280,
                    easing = LinearOutSlowInEasing
                )
            )
        }
        val subtitleOffsetJob = launch {
            subtitleOffset.animateTo(
                targetValue = 0f,
                animationSpec = tween(
                    durationMillis = 280,
                    easing = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)
                )
            )
        }

        // Giriş animasyonlarının tamamlanmasını bekle
        sktAlphaJob.join()
        sktScaleJob.join()
        glowJob.join()
        subtitleAlphaJob.join()
        subtitleOffsetJob.join()

        // Aşama 3: Logoların ekranda stabil kalması (~450 ms)
        delay(450)

        // Aşama 4: Birlikte yumuşak fade-out (~260 ms)
        val exitAlphaJob = launch {
            exitAlpha.animateTo(
                targetValue = 0f,
                animationSpec = tween(
                    durationMillis = 260,
                    easing = FastOutLinearInEasing
                )
            )
        }
        val exitScaleJob = launch {
            exitScale.animateTo(
                targetValue = 0.96f,
                animationSpec = tween(
                    durationMillis = 260,
                    easing = FastOutLinearInEasing
                )
            )
        }

        exitAlphaJob.join()
        exitScaleJob.join()

        // Splash tamamlandı, ana ekrana devret
        onSplashFinished()
    }

    // Tam siyah zemin ve dokunmaları emen koruma katmanı
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .graphicsLayer {
                alpha = exitAlpha.value
            }
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        event.changes.forEach { it.consume() }
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        // Ortadaki logo ve metin grubu
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.graphicsLayer {
                scaleX = exitScale.value
                scaleY = exitScale.value
            }
        ) {
            // Animasyon 3: SKT arkasında çok hafif, kurumsal Teal / Turkuaz ışık derinliği (abartısız, premium)
            Box(
                modifier = Modifier
                    .size(240.dp)
                    .graphicsLayer {
                        alpha = glowAlpha.value * 0.45f
                    }
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFF0D9488).copy(alpha = 0.28f),
                                Color(0xFF0D9488).copy(alpha = 0.08f),
                                Color.Transparent
                            )
                        ),
                        shape = CircleShape
                    )
            )

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Animasyon 1: "SKT" Ana Başlık (Fade + Scale)
                Text(
                    text = "SKT",
                    color = Color.White,
                    fontSize = 54.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.SansSerif,
                    letterSpacing = 2.sp,
                    modifier = Modifier.graphicsLayer {
                        alpha = sktAlpha.value
                        scaleX = sktScale.value
                        scaleY = sktScale.value
                    }
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Animasyon 2: "Takip & Stok" Alt Başlık (Hafif harf aralıklı fade-in)
                Text(
                    text = "Takip & Stok",
                    color = Color(0xFFE2E8F0),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.SansSerif,
                    letterSpacing = 3.5.sp,
                    modifier = Modifier.graphicsLayer {
                        alpha = subtitleAlpha.value
                        translationY = subtitleOffset.value
                    }
                )
            }
        }
    }
}
