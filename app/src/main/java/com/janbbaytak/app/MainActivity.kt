package com.janbbaytak.app

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.Build
import android.view.View
import android.webkit.*
import android.widget.FrameLayout
import org.json.JSONObject
import androidx.activity.OnBackPressedCallback
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import com.google.firebase.messaging.FirebaseMessaging

class MainActivity : ComponentActivity() {
    private lateinit var webView: WebView
    private var filePathCallback: ValueCallback<Array<Uri>>? = null
    private var pendingAuthUri: Uri? = null
    private var webPageReady = false

    private val imagePicker = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        filePathCallback?.onReceiveValue(uri?.let { arrayOf(it) })
        filePathCallback = null
    }

    private val notificationPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) notifyWebViewPushReady()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pendingAuthUri = intent?.data
        window.statusBarColor = Color.rgb(13, 89, 64)
        window.navigationBarColor = Color.rgb(13, 89, 64)

        webView = WebView(this)
        webView.layoutParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        )
        webView.setBackgroundColor(Color.rgb(246, 248, 246))
        webView.addJavascriptInterface(NativePushBridge(), "JanbNativePush")
        webView.addJavascriptInterface(NativeAuthBridge(), "JanbNativeAuth")
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
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                webPageReady = true
                consumeAuthCallback()
            }
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
        setupFirebaseMessaging()
        loadLatestInterface()
        checkForAppUpdate()

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val script = """
                    (function(){
                      if (typeof window.handleAppBack === 'function') return window.handleAppBack();
                      const ids=['productModal','cartModal','adModal','loginModal','signupModal','businessPage','ownerPage','adminPage','searchPage','categoryPage','adDetailPage'];
                      for (const id of ids) { const e=document.getElementById(id); if(e&&e.classList.contains('show')) { e.classList.remove('show'); return true; } }
                      const store=document.getElementById('storePage'); if(store&&store.classList.contains('show')){store.classList.remove('show');return true;}
                      return false;
                    })();
                """.trimIndent()
                webView.evaluateJavascript(script) { result ->
                    if (result != "true") finish()
                }
            }
        })
    }

    private inner class NativeAuthBridge {
        @JavascriptInterface
        fun isNativeApp(): Boolean = true
    }

    private inner class NativePushBridge {
        @JavascriptInterface
        fun getFcmToken(): String = getSharedPreferences("janb_push", MODE_PRIVATE).getString("fcm_token", "") ?: ""

        @JavascriptInterface
        fun notificationsAllowed(): Boolean = Build.VERSION.SDK_INT < 33 ||
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    }

    private fun setupFirebaseMessaging() {
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            val prefs = getSharedPreferences("janb_push", MODE_PRIVATE)
            if (!prefs.getBoolean("notification_permission_asked", false)) {
                prefs.edit().putBoolean("notification_permission_asked", true).apply()
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                getSharedPreferences("janb_push", MODE_PRIVATE).edit().putString("fcm_token", task.result).apply()
                notifyWebViewPushReady()
            }
        }
    }

    private fun notifyWebViewPushReady() {
        if (!::webView.isInitialized) return
        webView.post { webView.evaluateJavascript("window.syncNativePushToken && window.syncNativePushToken()", null) }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingAuthUri = intent.data
        consumeAuthCallback()
    }

    private fun consumeAuthCallback() {
        if (!webPageReady || !::webView.isInitialized) return
        val uri = pendingAuthUri ?: return
        if (uri.scheme != "janbbaytak" || uri.host != "auth" || uri.path != "/callback") return
        val rawParams = uri.fragment ?: uri.encodedQuery ?: return
        val params = Uri.parse("https://callback.local/?$rawParams")
        val accessToken = params.getQueryParameter("access_token") ?: return
        val refreshToken = params.getQueryParameter("refresh_token") ?: return
        val access = JSONObject.quote(accessToken)
        val refresh = JSONObject.quote(refreshToken)
        webView.evaluateJavascript(
            "(function(){if(typeof window.handleNativeAuthCallback!=='function')return 'wait';window.handleNativeAuthCallback($access,$refresh);return 'called';})()"
        ) { result ->
            if (result == "\"called\"") pendingAuthUri = null
        }
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    private fun compareVersions(first: String, second: String): Int {
        val a = Regex("\\d+").findAll(first).map { it.value.toIntOrNull() ?: 0 }.toList()
        val b = Regex("\\d+").findAll(second).map { it.value.toIntOrNull() ?: 0 }.toList()
        for (i in 0 until maxOf(a.size, b.size)) {
            val left = a.getOrElse(i) { 0 }
            val right = b.getOrElse(i) { 0 }
            if (left != right) return left.compareTo(right)
        }
        return 0
    }

    private fun checkForAppUpdate() {
        Thread {
            try {
                val url = java.net.URL("https://api.github.com/repos/hassanabozainab2-pixel/janb-baytak/releases/latest")
                val connection = url.openConnection()
                connection.connectTimeout = 10000
                connection.readTimeout = 10000
                connection.setRequestProperty("Accept", "application/vnd.github+json")
                connection.setRequestProperty("User-Agent", "JanbBaytak-Android")
                val json = JSONObject(connection.getInputStream().bufferedReader().use { it.readText() })
                val latestTag = json.optString("tag_name")
                val assets = json.optJSONArray("assets") ?: return@Thread
                var apkUrl: String? = null
                for (i in 0 until assets.length()) {
                    val asset = assets.optJSONObject(i) ?: continue
                    if (asset.optString("name") == "janb-baytak.apk") {
                        apkUrl = asset.optString("browser_download_url")
                        break
                    }
                }
                val currentVersion = packageManager.getPackageInfo(packageName, 0).versionName ?: ""
                if (latestTag.isNotBlank() && compareVersions(latestTag.removePrefix("v"), currentVersion) > 0 && !apkUrl.isNullOrBlank()) {
                    runOnUiThread {
                        android.app.AlertDialog.Builder(this)
                            .setTitle("تحديث جديد لجنب بيتك")
                            .setMessage("في إصدار أحدث من التطبيق. تحب تنزّله دلوقتي؟")
                            .setPositiveButton("فتح رابط التحديث") { _, _ ->
                                try {
                                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(apkUrl!!)))
                                } catch (_: Exception) {
                                    android.widget.Toast.makeText(this, "تعذر فتح رابط التحديث.", android.widget.Toast.LENGTH_LONG).show()
                                }
                            }
                            .setNegativeButton("لاحقًا", null)
                            .show()
                    }
                }
            } catch (_: Exception) {
                // Keep the app usable when the update service or internet is unavailable.
            }
        }.start()
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
                        "https://raw.githack.com/hassanabozainab2-pixel/janb-baytak/main/web/",
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
