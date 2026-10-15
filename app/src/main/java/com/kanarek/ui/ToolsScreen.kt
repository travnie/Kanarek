package com.kanarek.ui

import android.Manifest
import android.graphics.Bitmap
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.webkit.CookieManager
import android.webkit.PermissionRequest
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.kanarek.R

// One native hub. Bench code remains owned by travnie/twojstar.
private fun isTrustedOrigin(target: Uri, trusted: Uri): Boolean =
    target.scheme == "https" &&
        target.host?.equals(trusted.host, ignoreCase = true) == true &&
        (if (target.port == -1) 443 else target.port) ==
        (if (trusted.port == -1) 443 else trusted.port)

private enum class Bench(
    val label: String,
    val summaryRes: Int,
    val url: String,
    val icon: ImageVector,
) {
    CODE("Codebench", R.string.tools_codebench_description, "https://codebench.trfny.com", Icons.Filled.QrCodeScanner),
    DOC("Docbench", R.string.tools_docbench_description, "https://docbench.travny.workers.dev", Icons.Filled.Description),
    STREAM("Streambench", R.string.tools_streambench_description, "https://streambench.trfny.com", Icons.Filled.Tv),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ToolsScreen(
    onMenu: () -> Unit,
    isActive: Boolean,
) {
    var selectedKey by rememberSaveable { mutableStateOf<String?>(null) }
    val selected = Bench.entries.firstOrNull { it.name == selectedKey }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(selected?.label ?: stringResource(R.string.tools_title)) },
                navigationIcon = {
                    IconButton(onClick = if (selected == null) onMenu else ({ selectedKey = null })) {
                        Icon(
                            if (selected == null) Icons.Filled.Menu else Icons.Filled.ArrowBack,
                            contentDescription = stringResource(
                                if (selected == null) R.string.menu else R.string.tools_back,
                            ),
                        )
                    }
                },
            )
        },
    ) { padding ->
        if (selected == null) {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    Text(
                        stringResource(R.string.tools_intro),
                        modifier = Modifier.padding(vertical = 12.dp),
                    )
                }
                items(Bench.entries) { bench ->
                    ElevatedCard(
                        onClick = { selectedKey = bench.name },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column {
                            ListItem(
                                leadingContent = {
                                    Icon(bench.icon, contentDescription = null)
                                },
                                headlineContent = { Text(bench.label) },
                                supportingContent = {
                                    Text(stringResource(bench.summaryRes))
                                },
                                trailingContent = { Text(stringResource(R.string.tools_open)) },
                            )
                            Text(
                                stringResource(R.string.tools_online_hint),
                                modifier = Modifier.padding(start = 16.dp, bottom = 12.dp),
                            )
                        }
                    }
                }
            }
        } else if (isActive) {
            BenchWebView(
                bench = selected,
                modifier = Modifier.fillMaxSize().padding(padding),
                onExit = { selectedKey = null },
            )
        }
    }
}

// Remote origins receive no JavaScript bridge. Local bundled builds and native
// import/export use a separate, explicit bridge in a later phase.
@Composable
private fun BenchWebView(
    bench: Bench,
    modifier: Modifier = Modifier,
    onExit: () -> Unit,
) {
    val context = LocalContext.current
    val trustedOrigin = remember(bench) { Uri.parse(bench.url) }
    var fileCallback by remember { mutableStateOf<ValueCallback<Array<Uri>>?>(null) }
    var cameraRequest by remember { mutableStateOf<PermissionRequest?>(null) }

    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        fileCallback?.onReceiveValue(
            WebChromeClient.FileChooserParams.parseResult(result.resultCode, result.data),
        )
        fileCallback = null
    }
    val cameraPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { allowed ->
        cameraRequest?.let { request ->
            if (allowed) request.grant(arrayOf(PermissionRequest.RESOURCE_VIDEO_CAPTURE)) else request.deny()
        }
        cameraRequest = null
    }

    val webView = remember(bench) {
        WebView(context).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.allowFileAccess = false
            // Content URIs from the system picker are explicitly user-selected.
            settings.allowContentAccess = true
            settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
            settings.mediaPlaybackRequiresUserGesture = true
            CookieManager.getInstance().setAcceptThirdPartyCookies(this, false)

            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                    val target = request?.url ?: return true
                    if (isTrustedOrigin(target, trustedOrigin)) return false
                    if (request.isForMainFrame && target.scheme == "https") {
                        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, target)) }
                    }
                    return true
                }

                // shouldOverrideUrlLoading is not called for POST navigation.
                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                    val target = url?.let(Uri::parse)
                    if (target == null || !isTrustedOrigin(target, trustedOrigin)) view?.stopLoading()
                }
            }
            webChromeClient = object : WebChromeClient() {
                override fun onShowFileChooser(
                    webView: WebView?,
                    filePathCallback: ValueCallback<Array<Uri>>?,
                    fileChooserParams: FileChooserParams?,
                ): Boolean {
                    if (filePathCallback == null) return false
                    val currentPage = webView?.url?.let(Uri::parse)
                    if (fileCallback != null || currentPage == null ||
                        !isTrustedOrigin(currentPage, trustedOrigin)
                    ) {
                        filePathCallback.onReceiveValue(null)
                        return true
                    }
                    fileCallback = filePathCallback
                    val chooser = fileChooserParams?.createIntent() ?: Intent(Intent.ACTION_GET_CONTENT).apply {
                        type = "*/*"
                        addCategory(Intent.CATEGORY_OPENABLE)
                    }
                    runCatching { filePicker.launch(chooser) }.onFailure {
                        fileCallback?.onReceiveValue(null)
                        fileCallback = null
                    }
                    return true
                }

                override fun onPermissionRequest(request: PermissionRequest) {
                    val origin = request.origin
                    if (bench != Bench.CODE || !isTrustedOrigin(origin, trustedOrigin) ||
                        request.resources.toSet() != setOf(PermissionRequest.RESOURCE_VIDEO_CAPTURE)
                    ) {
                        request.deny()
                        return
                    }
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                        PackageManager.PERMISSION_GRANTED
                    ) {
                        request.grant(arrayOf(PermissionRequest.RESOURCE_VIDEO_CAPTURE))
                    } else {
                        cameraRequest?.deny()
                        cameraRequest = request
                        cameraPermission.launch(Manifest.permission.CAMERA)
                    }
                }

                override fun onPermissionRequestCanceled(request: PermissionRequest) {
                    if (cameraRequest === request) cameraRequest = null
                }
            }
            loadUrl(bench.url)
        }
    }

    BackHandler {
        if (webView.canGoBack()) webView.goBack() else onExit()
    }
    DisposableEffect(webView) {
        onDispose {
            fileCallback?.onReceiveValue(null)
            fileCallback = null
            cameraRequest?.deny()
            cameraRequest = null
        }
    }
    AndroidView(
        factory = { webView },
        modifier = modifier,
        onReset = null,
        onRelease = { released ->
            released.stopLoading()
            released.destroy()
        },
    )
}
