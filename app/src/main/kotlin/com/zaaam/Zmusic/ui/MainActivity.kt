package com.zaaam.Zmusic.ui

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.zaaam.Zmusic.ui.theme.ZmusicTheme
import com.zaaam.Zmusic.ui.zrun.ZRunApp
import dagger.hilt.android.AndroidEntryPoint

/**
 * Entry point ZRun. Seluruh UI ada di ui/zrun/ (design system Ember);
 * ZmusicTheme hanya menyediakan MaterialTheme dasar untuk komponen M3
 * (dialog, slider, dll).
 */
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ZmusicTheme {
                ZRunApp()
            }
        }
    }
}
