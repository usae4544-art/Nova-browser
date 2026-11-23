package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.components.NovaWallpaperBackground
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.LuminousViolet
import kotlinx.coroutines.delay

@Composable
fun SplashOverlay(
    visible: Boolean,
    onFinished: () -> Unit
) {
    val scale = remember { Animatable(0.84f) }
    val alpha = remember { Animatable(0f) }

    LaunchedEffect(visible) {
        if (visible) {
            alpha.animateTo(1f, animationSpec = tween(320, easing = FastOutSlowInEasing))
            scale.animateTo(1f, animationSpec = tween(380, easing = FastOutSlowInEasing))
            delay(480L)
            onFinished()
        }
    }

    AnimatedVisibility(
        visible = visible,
        exit = fadeOut(animationSpec = tween(280))
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            NovaWallpaperBackground(isIncognito = false)

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .scale(scale.value)
                    .alpha(alpha.value)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(116.dp)
                        .shadow(28.dp, RoundedCornerShape(32.dp), ambientColor = ElectricCyan, spotColor = LuminousViolet)
                        .clip(RoundedCornerShape(32.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFF0B0D24), Color(0xFF171C42))
                            )
                        )
                        .border(
                            width = 1.5.dp,
                            brush = Brush.linearGradient(listOf(ElectricCyan, LuminousViolet)),
                            shape = RoundedCornerShape(32.dp)
                        )
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_nova_icon_fg),
                        contentDescription = stringResource(R.string.cd_nova_logo),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(92.dp)
                            .clip(RoundedCornerShape(24.dp))
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = stringResource(R.string.nova_brand_display),
                    style = MaterialTheme.typography.displayLarge.copy(
                        brush = Brush.horizontalGradient(listOf(ElectricCyan, Color.White, LuminousViolet))
                    )
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = stringResource(R.string.nova_brand_subtitle),
                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 3.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
