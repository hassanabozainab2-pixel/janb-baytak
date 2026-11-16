package com.janbbaytak.app

import android.Manifest
import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.IntentFilter
import android.os.Environment
import android.provider.Settings
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
    private val updatePrefs by lazy { getSharedPreferences("janb_app_update", MODE_PRIVATE) }
    private val updateDownloadReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val completedId = intent?.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L) ?: -1L
            val expectedId = updatePrefs.getLong("download_id", -1L)
            if (completedId == expectedId && completedId != -1L) installDownloadedUpdate(completedId)
        }
    }

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
        val updateFilter = IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
        if (Build.VERSION.SDK_INT >= 33) {
            registerReceiver(updateDownloadReceiver, updateFilter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("DEPRECATION")
            registerReceiver(updateDownloadReceiver, updateFilter)
        }
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
        val uri = pendingAuthUri ?: return
        if (uri.scheme != "janbbaytak" || uri.host != "auth" || uri.path != "/callback") return
        val fragment = uri.fragment ?: return
        val params = Uri.parse("https://callback.local/?$fragment")
        val accessToken = params.getQueryParameter("access_token") ?: return
        val refreshToken = params.getQueryParameter("refresh_token") ?: return
        pendingAuthUri = null
        val access = JSONObject.quote(accessToken)
        val refresh = JSONObject.quote(refreshToken)
        webView.evaluateJavascript("window.handleNativeAuthCallback && window.handleNativeAuthCallback($access,$refresh)", null)
    }

    override fun onResume() {
        super.onResume()
        val pendingId = updatePrefs.getLong("download_id", -1L)
        if (pendingId != -1L && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && packageManager.canRequestPackageInstalls()) {
            installDownloadedUpdate(pendingId)
        }
    }

    override fun onDestroy() {
        try { unregisterReceiver(updateDownloadReceiver) } catch (_: Exception) {}
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

    private fun downloadUpdate(apkUrl: String) {
        try {
            val request = DownloadManager.Request(Uri.parse(apkUrl))
                .setTitle("تحديث جنب بيتك")
                .setDescription("جاري تنزيل التحديث الجديد...")
                .setMimeType("application/vnd.android.package-archive")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setAllowedOverMetered(true)
                .setAllowedOverRoaming(false)
                .setDestinationInExternalFilesDir(this, Environment.DIRECTORY_DOWNLOADS, "janb-baytak-update.apk")
            val manager = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            val id = manager.enqueue(request)
            updatePrefs.edit().putLong("download_id", id).apply()
            android.widget.Toast.makeText(this, "بدأ تنزيل التحديث. هتظهر لك رسالة التثبيت بعد ما يكتمل.", android.widget.Toast.LENGTH_LONG).show()
        } catch (_: Exception) {
            android.widget.Toast.makeText(this, "تعذر بدء تنزيل التحديث. حاول مرة أخرى.", android.widget.Toast.LENGTH_LONG).show()
        }
    }

    private fun installDownloadedUpdate(downloadId: Long) {
        try {
            val manager = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            val query = DownloadManager.Query().setFilterById(downloadId)
            manager.query(query).use { cursor ->
                if (!cursor.moveToFirst()) return
                val status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
                if (status != DownloadManager.STATUS_SUCCESSFUL) return
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !packageManager.canRequestPackageInstalls()) {
                android.app.AlertDialog.Builder(this)
                    .setTitle("السماح بتثبيت التحديث")
                    .setMessage("فعّل السماح بتثبيت التطبيقات من هذا المصدر مرة واحدة، وبعدها ارجع للتطبيق لإكمال التحديث.")
                    .setPositiveButton("فتح الإعدادات") { _, _ ->
                        startActivity(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:$packageName")))
                    }
                    .setNegativeButton("لاحقًا", null)
                    .show()
                return
            }
            val apkUri = manager.getUriForDownloadedFile(downloadId) ?: return
            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(installIntent)
            updatePrefs.edit().remove("download_id").apply()
        } catch (_: Exception) {
            android.widget.Toast.makeText(this, "التحديث اتنزّل، لكن تعذر فتح شاشة التثبيت. افتح إشعار التنزيل وحاول التثبيت.", android.widget.Toast.LENGTH_LONG).show()
        }
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
                            .setPositiveButton("تنزيل التحديث") { _, _ ->
                                downloadUpdate(apkUrl!!)
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
