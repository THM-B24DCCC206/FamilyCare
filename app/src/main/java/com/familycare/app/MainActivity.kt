package com.familycare.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.familycare.app.presentation.navigation.AppNavHost
import com.familycare.app.ui.theme.FamilyCareTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FamilyCareTheme {
                AppNavHost()
            }
        }
    }
}
