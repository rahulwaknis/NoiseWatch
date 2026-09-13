package com.example.noisewatch

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.example.noisewatch.ui.navigation.NoiseWatchApp
import com.example.noisewatch.ui.theme.NoiseWatchTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NoiseWatchTheme {
                NoiseWatchApp()
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AppPreview() {
    NoiseWatchTheme {
        NoiseWatchApp()
    }
}
