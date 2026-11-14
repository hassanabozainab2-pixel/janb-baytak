package com.janbbaytak.app

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.webkit.*
import android.widget.FrameLayout
import androidx.activity.OnBackPressedCallback
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts

class MainActivity : ComponentActivity() {
    private lateinit var webView: WebView
    private var filePathCallback: ValueCallback<Array<Uri>>? = null

    private val imagePicker = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        filePathCallback?.onReceiveValue(uri?.let { arrayOf(it) })
        filePathCallback = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.rgb(13, 89, 64)
        window.navigationBarColor = Color.rgb(13, 89, 64)

        webView = WebView(this)
        webView.layoutParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        )
        webView.setBackgroundColor(Color.rgb(246, 248, 246))
        webView.visibility = View.VISIBLE

        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            allowFileAccess = true
            allowContentAccess = true
            javaScriptCanOpenWindowsAutomatically = true
            setSupportMultipleWindows(false)
            loadsImagesAutomatically = true
            mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
            mediaPlaybackRequiresUserGesture = true
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onShowFileChooser(
                view: WebView?,
                callback: ValueCallback<Array<Uri>>?,
                params: FileChooserParams?
            ): Boolean {
                filePathCallback?.onReceiveValue(null)
                filePathCallback = callback
                imagePicker.launch("image/*")
                return true
            }
        }

        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                val uri = request?.url ?: return false
                val scheme = uri.scheme ?: return false
                if (scheme == "tel" || scheme == "mailto" || scheme == "sms" ||
                    uri.host == "wa.me" || uri.host == "api.whatsapp.com") {
                    return try {
                        startActivity(Intent(Intent.ACTION_VIEW, uri))
                        true
                    } catch (_: Exception) {
                        false
                    }
                }
                if (scheme == "http" || scheme == "https") {
                    val host = uri.host ?: ""
                    if (host == "raw.githubusercontent.com" || host.endsWith("supabase.co") ||
                        host.endsWith("supabase.in") || host.endsWith("jsdelivr.net") ||
                        host.endsWith("unsplash.com")) return false
                    return try {
                        startActivity(Intent(Intent.ACTION_VIEW, uri))
                        true
                    } catch (_: Exception) {
                        false
                    }
                }
                return false
            }
        }

        setContentView(webView)
        loadLatestInterface()

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val script = """
                    (function(){
                      const modals=['productModal','cartModal','adModal','businessModal','accountModal'];
                      for(let id of modals){const e=document.getElementById(id);if(e&&e.classList.contains('show')){e.classList.remove('show');return true;}}
                      const screens=['businessPage','ownerPage','adminPage','searchPage','categoryPage'];
                      for(let id of screens){const e=document.getElementById(id);if(e&&e.classList.contains('show')){e.classList.remove('show');if(id==='businessPage')history.replaceState({},'',location.pathname);return true;}}
                      return false;
                    })();
                """.trimIndent()
                webView.evaluateJavascript(script) { result ->
                    if (result != "true") {
                        if (webView.canGoBack()) webView.goBack() else finish()
                    }
                }
            }
        })
    }

    private fun loadLatestInterface() {
        Thread {
            var html: String? = null
            try {
                val url = java.net.URL(
                    "https://raw.githubusercontent.com/hassanabozainab2-pixel/janb-baytak/main/web/index.html"
                )
                val connection = url.openConnection()
                connection.connectTimeout = 15000
                connection.readTimeout = 20000
                connection.setRequestProperty("Cache-Control", "no-cache")
                connection.setRequestProperty("Pragma", "no-cache")
                connection.setRequestProperty("User-Agent", "JanbBaytak-Android")
                html = connection.getInputStream().bufferedReader(Charsets.UTF_8).use { it.readText() }
                connection.getInputStream().close()
            } catch (_: Exception) {
                html = null
            }

            runOnUiThread {
                if (!html.isNullOrBlank() && html!!.contains("id=\"appShell\"")) {
                    webView.loadDataWithBaseURL(
                        "https://raw.githubusercontent.com/hassanabozainab2-pixel/janb-baytak/main/web/",
                        html!!,
                        "text/html",
                        "UTF-8",
                        null
                    )
                } else {
                    webView.loadUrl("file:///android_asset/index.html")
                }
            }
        }.start()
    }
}
