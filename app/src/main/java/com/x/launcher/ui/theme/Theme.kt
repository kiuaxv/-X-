package com.x.launcher.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val XDarkColorScheme = darkColorScheme(
 primary = XButterYellow,
 onPrimary = XBlack,
 secondary = XButterDeep,
 onSecondary = XBlack,
 tertiary = XTeal,
 onTertiary = XBlack,
 background = XBlack,
 onBackground = XWhite,
 surface = XCardDark,
 onSurface = XWhite,
 surfaceVariant = XDarkGray,
 onSurfaceVariant = XGrayLight,
 error = XRed,
 onError = XWhite
)

private val XLightColorScheme = lightColorScheme(
 primary = XButtonBlack,
 onPrimary = XWhite,
 secondary = XButterDeep,
 onSecondary = XBlack,
 tertiary = XTeal,
 onTertiary = XBlack,
 background = XButterLight,
 onBackground = XBlack,
 surface = XWhite,
 onSurface = XBlack,
 surfaceVariant = XButterYellow,
 onSurfaceVariant = XDarkGray,
 error = XRed,
 onError = XWhite
)

@Composable
fun XLauncherTheme(
 darkTheme: Boolean = isSystemInDarkTheme(),
 content: @Composable () -> Unit
) {
 MaterialTheme(
 colorScheme = if (darkTheme) XDarkColorScheme else XLightColorScheme,
 typography = Typography,
 content = content
 )
}
