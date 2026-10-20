package com.example.bcs230117_foodmanager.screens

import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
import android.content.res.Configuration
import android.content.res.Resources
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import java.util.Locale

/** Hai ngôn ngữ của app. Tên hiển thị viết bằng chính ngôn ngữ đó nên không cần dịch. */
enum class AppLanguage(val tag: String, val displayName: String) {
    ENGLISH("en", "English"),
    VIETNAMESE("vi", "Tiếng Việt")
}

/** Giữ ngôn ngữ đang chọn (mặc định English) và lưu lại để lần mở app sau vẫn nhớ. */
object LanguageManager {
    private const val PREFS_NAME = "BCS230117_settings"
    private const val KEY_LANGUAGE = "language"

    private var prefs: SharedPreferences? = null

    /** Ngôn ngữ hiện tại; là state nên đổi giá trị thì giao diện tự dựng lại. */
    var current by mutableStateOf(AppLanguage.ENGLISH)
        private set

    /** Gọi một lần trong MainActivity.onCreate, trước setContent. */
    fun init(context: Context) {
        val p = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs = p
        val saved = p.getString(KEY_LANGUAGE, null)
        current = AppLanguage.entries.firstOrNull { it.tag == saved } ?: AppLanguage.ENGLISH
        Locale.setDefault(Locale.forLanguageTag(current.tag))
    }

    fun set(language: AppLanguage) {
        current = language
        // Đổi Locale mặc định để định dạng giá (45,000 / 45.000) cũng theo ngôn ngữ
        Locale.setDefault(Locale.forLanguageTag(language.tag))
        prefs?.edit()?.putString(KEY_LANGUAGE, language.tag)?.apply()
    }
}

/**
 * Bọc toàn bộ giao diện: mọi stringResource bên trong lấy chữ theo ngôn ngữ đang chọn.
 * Dùng ContextWrapper nên vẫn giữ được Activity gốc (điều hướng, nút Back hoạt động bình thường).
 */
@Composable
fun LocalizedContent(content: @Composable () -> Unit) {
    val base = LocalContext.current
    val baseConfig = LocalConfiguration.current
    val language = LanguageManager.current

    val config = remember(language, baseConfig) {
        Configuration(baseConfig).apply { setLocale(Locale.forLanguageTag(language.tag)) }
    }
    val localizedContext = remember(config, base) {
        val localizedResources: Resources = base.createConfigurationContext(config).resources
        object : ContextWrapper(base) {
            override fun getResources(): Resources = localizedResources
        }
    }

    CompositionLocalProvider(
        LocalContext provides localizedContext,
        LocalConfiguration provides config
    ) {
        content()
    }
}