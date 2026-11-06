package com.example.fix_finder

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.fix_finder.data.local.LocaleHelper
import com.example.fix_finder.data.local.SessionManager
import com.example.fix_finder.data.model.UserSettings

class MainActivity : AppCompatActivity() {

    override fun attachBaseContext(newBase: Context) {
        val lang = try {
            val prefs = newBase.getSharedPreferences("fix_finder_session", MODE_PRIVATE)
            val json = prefs.getString("user_settings", null)
            if (json != null && json.contains("\"preferredLanguage\":\"zu\"")) "zu" else "en"
        } catch (_: Throwable) {
            "en"
        }
        val contextWithLocale = LocaleHelper.setLocale(newBase, lang)
        super.attachBaseContext(contextWithLocale)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            val sessionManager = SessionManager.getInstance(applicationContext)
            val settings = sessionManager.getSettings()
            applyTheme(settings.themeMode)
        } catch (_: Throwable) {
            // Fallback if settings loading fails
        }

        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, SplashFragment())
                .commit()
        }

        val mainView = findViewById<View>(R.id.main)
        if (mainView != null) {
            ViewCompat.setOnApplyWindowInsetsListener(mainView) { v, insets ->
                val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
                insets
            }
        }
    }

    companion object {
        fun applyTheme(themeMode: String) {
            try {
                val mode = when (themeMode) {
                    UserSettings.THEME_LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
                    UserSettings.THEME_DARK -> AppCompatDelegate.MODE_NIGHT_YES
                    else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                }
                AppCompatDelegate.setDefaultNightMode(mode)
            } catch (_: Throwable) {}
        }
    }
}
