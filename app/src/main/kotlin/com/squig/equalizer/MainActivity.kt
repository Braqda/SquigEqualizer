package com.squig.equalizer

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.core.content.ContextCompat
import com.squig.equalizer.service.AudioEqualizerService

class MainActivity : Activity() {

    private lateinit var web: WebView
    private var retries = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        }

        ContextCompat.startForegroundService(this, Intent(this, AudioEqualizerService::class.java))

        web = WebView(this).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true   // profiles are saved in localStorage
            webViewClient = object : WebViewClient() {
                // The server starts a moment after the service; retry until it answers.
                override fun onReceivedError(v: WebView, r: WebResourceRequest, e: WebResourceError) {
                    if (r.isForMainFrame && retries++ < 15) v.postDelayed({ v.loadUrl(URL) }, 400)
                }
            }
        }
        setContentView(web)
        web.loadUrl(URL)
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (web.canGoBack()) web.goBack() else super.onBackPressed()
    }

    private companion object { const val URL = "http://127.0.0.1:8080/" }
}
