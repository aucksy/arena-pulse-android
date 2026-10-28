package com.arenapulse.android.ui

import android.content.Context
import android.content.Intent

/** Fire the system share sheet with plain text. Safe to call from any composable context. */
fun shareText(context: Context, subject: String, body: String) {
    runCatching {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, body)
        }
        context.startActivity(Intent.createChooser(intent, "Share via"))
    }
}
