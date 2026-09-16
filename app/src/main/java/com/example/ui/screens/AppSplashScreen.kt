package com.example.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.TurquoisePrimary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Modern, sade ve profesyonel açılış (Splash) animasyonu.
 * Klasik SKT Kalkan logosu (ic_skt_shield_logo), yükleme göstergesi ve sürüm etiketi içerir.
 */
@Composable
fun AppSplashScreen(
    onSplashFinished: () -> Unit
) {
    // Animasyon durumları
    val logoAlpha = remember { Animatable(0.2f) }
    val logoScale = remember { Animatable(0.92f) }
    val glowAlpha = remember { Animatable(0.3f) }

    val footerAlpha = remember { Animatable(0f) }
    val exitAlpha = remember { Animatable(1f) }
    val exitScale = remember { Animatable(1f) }

    LaunchedEffect(Unit) {
        // Aşama 1: Logo ve arka turkuaz ışıltının akıcı girişi (~300 ms)
        val logoAlphaJob = launch {
            logoAlpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = 300,
                    easing = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)
                )
            )
        }
        val logoScaleJob = launch {
            logoScale.animateTo(
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

        // Aşama 2: Yükleme göstergesi ve versiyon etiketinin girişi (~250 ms)
        delay(60)
        val footerJob = launch {
            footerAlpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = 280,
                    easing = LinearOutSlowInEasing
                )
            )
        }

        logoAlphaJob.join()
        logoScaleJob.join()
        glowJob.join()
        footerJob.join()

        // Aşama 3: Logoların ekranda stabil kalması (~550 ms)
        delay(550)

        // Aşama 4: Birlikte yumuşak fade-out (~250 ms)
        val exitAlphaJob = launch {
            exitAlpha.animateTo(
                targetValue = 0f,
                animationSpec = tween(
                    durationMillis = 250,
                    easing = FastOutLinearInEasing
                )
            )
        }
        val exitScaleJob = launch {
            exitScale.animateTo(
                targetValue = 0.96f,
                animationSpec = tween(
                    durationMillis = 250,
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
            }
    ) {
        // Ortadaki kurumsal logo ve yükleme grubu
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = exitScale.value
                    scaleY = exitScale.value
                },
            contentAlignment = Alignment.Center
        ) {
            // Arka plandaki modern turkuaz ışık derinliği
            Box(
                modifier = Modifier
                    .size(280.dp)
                    .graphicsLayer {
                        alpha = glowAlpha.value * 0.5f
                    }
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFF0D9488).copy(alpha = 0.35f),
                                Color(0xFF0D9488).copy(alpha = 0.1f),
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
                // Klasik SKT Kalkan Logosu
                Image(
                    painter = painterResource(id = R.drawable.ic_skt_shield_logo),
                    contentDescription = "SKT Takip Logo",
                    modifier = Modifier
                        .size(160.dp)
                        .graphicsLayer {
                            alpha = logoAlpha.value
                            scaleX = logoScale.value
                            scaleY = logoScale.value
                        }
                )

                Spacer(modifier = Modifier.height(28.dp))

                // Şık Yükleme Göstergesi
                CircularProgressIndicator(
                    modifier = Modifier
                        .size(24.dp)
                        .graphicsLayer {
                            alpha = footerAlpha.value
                        },
                    color = TurquoisePrimary,
                    strokeWidth = 2.5.dp
                )
            }
        }

        // Alt kısımda Sürüm Etiketi
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 24.dp)
                .graphicsLayer {
                    alpha = footerAlpha.value
                }
        ) {
            Text(
                text = "Versiyon ${com.example.BuildConfig.VERSION_NAME}",
                color = Color(0xFF94A3B8),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 1.sp
            )
        }
    }
}
