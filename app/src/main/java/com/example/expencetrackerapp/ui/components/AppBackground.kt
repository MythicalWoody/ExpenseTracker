package com.example.expencetrackerapp.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource

@Composable
fun AppBackground(modifier: Modifier = Modifier) {
    val currentBackground = com.example.expencetrackerapp.ui.theme.ThemeState.currentBackground
    Box(modifier = modifier) {
        if (currentBackground ==
                        com.example.expencetrackerapp.ui.theme.AppBackgroundTheme.LIQUID_SYMPHONY
        ) {
            BackgroundPattern()
        } else if (currentBackground.drawableRes != 0) {
            Image(
                    painter = painterResource(id = currentBackground.drawableRes),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
            )
        }
    }
}
