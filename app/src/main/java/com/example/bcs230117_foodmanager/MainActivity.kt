package com.example.bcs230117_foodmanager

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.bcs230117_foodmanager.screens.AppNavigation
import com.example.bcs230117_foodmanager.screens.LanguageManager
import com.example.bcs230117_foodmanager.screens.LocalizedContent
import com.example.bcs230117_foodmanager.ui.theme.BCS230117_FoodManagerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Đọc ngôn ngữ đã lưu (mặc định English) trước khi vẽ giao diện
        LanguageManager.init(applicationContext)
        enableEdgeToEdge()
        setContent {
            BCS230117_FoodManagerTheme {
                LocalizedContent {
                    AppNavigation()
                }
            }
        }
    }
}