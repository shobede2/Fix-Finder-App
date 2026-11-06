package com.example.fix_finder.data.local

import android.util.Log
import android.view.View
import android.widget.Toast

fun View.safeClick(intervalMillis: Long = 600L, action: (View) -> Unit) {
    var lastClickTime = 0L
    setOnClickListener { v ->
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastClickTime >= intervalMillis) {
            lastClickTime = currentTime
            try {
                action(v)
            } catch (e: Exception) {
                Log.e("SafeClick", "Click action failed", e)
                Toast.makeText(v.context, "Action unavailable. Please try again.", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
