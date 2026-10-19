package com.kanarek.ui

import android.app.Activity
import android.Manifest
import android.graphics.Bitmap
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import org.json.JSONObject
import android.webkit.CookieManager
import android.webkit.PermissionRequest
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.TextButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import com.kanarek.files.BenchFileTarget
import com.kanarek.files.BenchImportException
import com.kanarek.files.IncomingBenchFile
import com.kanarek.files.IncomingBenchFiles
import com.kanarek.files.PreparedBenchFile
import com.kanarek.data.ScanPayload
import com.kanarek.data.ScanPayloadParser
import com.kanarek.R
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// One native hub. Bench code remains owned by travnie/twojstar.
// Observe Codebench's visible scan history, not third-party pages or page data.
// Result only opens an Android confirmation UI; this bridge has no native actions.
private val codeScanObserver = """
(() => {
  if (window.__kanarekScanObserver) return;
  window.__kanarekScanObserver = true;
  const install = () => {
    const history = document.getElementById('scanHist');
    if (!history) return;
    new MutationObserver(records => {
      if (!records.some(record => [...record.addedNodes].some(node =>
          node.nodeType === 1 && node.classList.contains('item')))) return;
      const value = document.getElementById('resVal')?.textContent || '';
      const format = document.getElementById('resFmt')?.textContent || '';
      if (!value || value.length > 16384) return;
      const stop = document.getElementById('camStop');
      if (stop && !stop.disabled) stop.click();
      window.KanarekScan?.postMessage(JSON.stringify({v: 1, text: value, format}));
    }).observe(history, {childList: true});
  };
  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', install, {once: true});
  } else install();
})();
""".trimIndent()

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
    val kind: BenchKind,
) {
    CODE("Codebench", R.string.tools_codebench_description, "https://codebench.trfny.com", Icons.Filled.QrCodeScanner, BenchKind.CODE),
    DOC("Docbench", R.string.tools_docbench_description, "https://docbench.travny.workers.dev", Icons.Filled.Description, BenchKind.DOC),
    STREAM("Streambench", R.string.tools_streambench_description, "https://streambench.trfny.com", Icons.Filled.Tv, BenchKind.STREAM),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ToolsScreen(
    onMenu: () -> Unit,
    isActive: Boolean,
    pendingScanRequest: Int,
    onScanRequestConsumed: (Int) -> Unit,
    inboundFile: IncomingBenchFile?,
    onFileHandled: (Long) -> Unit,
) {
    val context = LocalContext.current
    val bundledBenches = remember(context) {
        Bench.entries.associateWith { OfflineBenches.isBundled(context, it.kind) }
    }
    val scope = rememberCoroutineScope()
    val importFailureMessage = stringResource(R.string.open_with_failed)
    val tooLargeMessage = stringResource(R.string.open_with_too_large)
    var openingFile by remember { mutableStateOf(false) }
    var fileError by remember { mutableStateOf<String?>(null) }
    var preparedFile by remember { mutableStateOf<PreparedBenchFile?>(null) }
    var processingFileId by remember { mutableStateOf<Long?>(null) }
    var selectedKey by rememberSaveable { mutableStateOf<String?>(null) }
    var useHosted by rememberSaveable { mutableStateOf(false) }
    val selected = Bench.entries.firstOrNull { it.name == selectedKey }
    var scanned by remember { mutableStateOf<ScanPayload?>(null) }
    LaunchedEffect(pendingScanRequest, inboundFile?.id, processingFileId) {
        // A file import in progress owns the page until it is delivered.
        if (pendingScanRequest > 0 && inboundFile == null &&
            processingFileId == null && preparedFile == null
        ) {
            selectedKey = Bench.CODE.name
            useHosted = false
        }
    }

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
                actions = {
                    if (selected != null && bundledBenches[selected] == true) {
                        TextButton(
                            enabled = !openingFile && preparedFile == null,
                            onClick = { useHosted = !useHosted },
                        ) {
                            Text(stringResource(
                                if (useHosted) R.string.tools_switch_offline
                                else R.string.tools_switch_online,
                            ))
                        }
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
                        stringResource(if (bundledBenches.values.all { it }) R.string.tools_offline_intro else R.string.tools_intro),
                        modifier = Modifier.padding(vertical = 12.dp),
                    )
                }
                item {
                    Text(
                        stringResource(if (bundledBenches.values.all { it }) R.string.tools_all_offline_hint else R.string.tools_online_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 4.dp),
                    )
                }
                items(Bench.entries) { bench ->
                    ElevatedCard(
                        onClick = {
                            preparedFile = null
                            useHosted = false
                            selectedKey = bench.name
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        ListItem(
                            leadingContent = { Icon(bench.icon, contentDescription = null) },
                            headlineContent = { Text(bench.label) },
                            supportingContent = { Text(stringResource(bench.summaryRes)) },
                            trailingContent = {
                                Text(stringResource(
                                    if (bundledBenches[bench] == true) R.string.tools_offline_badge
                                    else R.string.tools_open,
                                ))
                            },
                        )
                    }
                }
            }
        } else if (isActive) {
            BenchWebView(
                bench = selected,
                useHosted = useHosted,
                pendingScanRequest = pendingScanRequest,
                onScanRequestConsumed = onScanRequestConsumed,
                modifier = Modifier.fillMaxSize().padding(padding),
                onExit = {
                    preparedFile?.let { onFileHandled(it.id) }
                    preparedFile = null
                    processingFileId = null
                    selectedKey = null
                    useHosted = false
                },
                onScan = { scanned = it },
                incomingFile = preparedFile?.takeIf { it.target.name == benchKey(selected) },
                onFileImported = { imported ->
                    preparedFile?.let { onFileHandled(it.id) }
                    preparedFile = null
                    processingFileId = null
                    if (!imported) fileError = importFailureMessage
                },
            )
        }
    }
    if (isActive) {
        inboundFile?.takeIf { it.id != processingFileId }?.let { source ->
            val options = when (source.suggested) {
                BenchFileTarget.CODE -> listOf(BenchFileTarget.CODE)
                BenchFileTarget.STREAM -> listOf(BenchFileTarget.STREAM, BenchFileTarget.DOC)
                BenchFileTarget.DOC -> if (source.filename.endsWith(".xml", true) ||
                    source.mime.equals("application/xml", true) ||
                    source.mime.equals("text/xml", true)
                ) listOf(BenchFileTarget.DOC, BenchFileTarget.STREAM)
                else listOf(BenchFileTarget.DOC)
            }
            AlertDialog(
                onDismissRequest = { onFileHandled(source.id) },
                title = { Text(stringResource(R.string.open_with_title)) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(source.filename)
                        Text(stringResource(
                            if (bundledBenches.values.any { it }) R.string.open_with_offline_privacy
                            else R.string.open_with_privacy,
                        ))
                        options.forEach { target ->
                            TextButton(onClick = {
                                processingFileId = source.id
                                scope.launch {
                                    openingFile = true
                                    try {
                                        val ready = IncomingBenchFiles.prepare(context, source, target)
                                        preparedFile = ready
                                        useHosted = false
                                        selectedKey = target.name
                                    } catch (error: Exception) {
                                        fileError =
                                            if ((error as? BenchImportException)?.reason ==
                                                BenchImportException.Reason.TOO_LARGE
                                            ) tooLargeMessage else importFailureMessage
                                        processingFileId = null
                                        onFileHandled(source.id)
                                    } finally {
                                        openingFile = false
                                    }
                                }
                            }) {
                                Text(Bench.valueOf(target.name).label)
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { onFileHandled(source.id) }) {
                        Text(stringResource(R.string.open_with_cancel))
                    }
                },
            )
        }
        if (openingFile) {
            AlertDialog(
                onDismissRequest = {},
                title = { Text(stringResource(R.string.open_with_loading)) },
                text = { CircularProgressIndicator() },
                confirmButton = {},
            )
        }
        fileError?.let { message ->
            AlertDialog(
                onDismissRequest = { fileError = null },
                title = { Text(stringResource(R.string.open_with_failed_title)) },
                text = { Text(message) },
                confirmButton = {
                    TextButton(onClick = { fileError = null }) {
                        Text(stringResource(R.string.open_with_cancel))
                    }
                },
            )
        }
        scanned?.let { payload ->
            ScanResultDialog(payload = payload, onDismiss = { scanned = null })
        }
    }
}

private fun benchKey(bench: Bench): String = bench.name

// Only Codebench's pinned HTTPS origin can report scanned text. No remote
// page can invoke Android actions through the bridge.
@Composable
private fun BenchWebView(
    bench: Bench,
    useHosted: Boolean,
    pendingScanRequest: Int,
    onScanRequestConsumed: (Int) -> Unit,
    modifier: Modifier = Modifier,
    onExit: () -> Unit,
    onScan: (ScanPayload) -> Unit,
    incomingFile: PreparedBenchFile?,
    onFileImported: (Boolean) -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val benchKind = bench.kind
    val offline = remember(bench, context, useHosted) {
        !useHosted && OfflineBenches.isBundled(context, benchKind)
    }
    val pageUrl = OfflineBenches.destination(offline, benchKind, bench.url)
    val trustedOrigin = remember(pageUrl) { Uri.parse(pageUrl) }
    val assetLoader = remember(offline, context) {
        if (offline) OfflineBenches.loader(context, benchKind) else null
    }
    fun trustedPage(target: Uri): Boolean =
        if (offline) OfflineBenches.isTrustedPage(target, benchKind)
        else isTrustedOrigin(target, trustedOrigin)
    var fileCallback by remember { mutableStateOf<ValueCallback<Array<Uri>>?>(null) }
    var cameraRequest by remember { mutableStateOf<PermissionRequest?>(null) }
    var pendingExport by remember(bench) { mutableStateOf<BenchExport?>(null) }
    var exportFailed by remember(bench) { mutableStateOf(false) }
    val receiver = remember(bench) { BenchExportReceiver() }
    var startedScanRequest by remember(bench) { mutableIntStateOf(0) }
    val currentScanRequest by rememberUpdatedState(pendingScanRequest)
    val currentConsume by rememberUpdatedState(onScanRequestConsumed)
    val currentFile by rememberUpdatedState(incomingFile)
    val currentImportFinished by rememberUpdatedState(onFileImported)
    var dispatchedFileId by remember(bench) { mutableStateOf<Long?>(null) }

    fun deliverFileIfReady(view: WebView?) {
        val incoming = currentFile ?: return
        if (view == null || view.progress < 100 ||
            incoming.target.name != bench.name || dispatchedFileId == incoming.id
        ) return
        val currentUrl = view.url?.let(Uri::parse) ?: return
        if (!trustedPage(currentUrl)) return
        dispatchedFileId = incoming.id
        view.evaluateJavascript(IncomingBenchFiles.importJavascript(incoming)) { result ->
            currentImportFinished(result?.trim() == "true")
        }
    }

    fun startCameraIfRequested(view: WebView?) {
        val requested = currentScanRequest
        if (bench != Bench.CODE || view == null ||
            requested == 0 || requested <= startedScanRequest || view.progress < 100
        ) return
        startedScanRequest = requested
        currentConsume(requested)
        view.evaluateJavascript(
            "(function(){const start=document.getElementById('camStart');if(start&&!start.disabled)start.click();})();",
            null,
        )
    }

    val exportPicker = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val payload = pendingExport
        pendingExport = null
        val uri = result.data?.data
        if (result.resultCode == Activity.RESULT_OK && uri != null && payload != null) {
            scope.launch {
                val saved = withContext(Dispatchers.IO) {
                    runCatching {
                        context.contentResolver.openOutputStream(uri, "w")?.use {
                            it.write(payload.bytes)
                        } ?: error("Document provider refused export")
                    }.isSuccess
                }
                if (!saved) exportFailed = true
            }
        }
    }
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

    val webView = remember(bench, useHosted) {
        WebView(context).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.allowFileAccess = false
            // Content URIs from the system picker are explicitly user-selected.
            settings.allowContentAccess = true
            settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
            settings.mediaPlaybackRequiresUserGesture = true
            CookieManager.getInstance().setAcceptThirdPartyCookies(this, false)

            val scanOrigins = setOf(if (offline) OfflineBenches.origin(benchKind) else Bench.CODE.url)
            if (offline && WebViewFeature.isFeatureSupported(WebViewFeature.WEB_MESSAGE_LISTENER)) {
                val originRules = setOf(OfflineBenches.origin(benchKind))
                WebViewCompat.addWebMessageListener(
                    this, "KanarekExport", originRules,
                ) { view, message, origin, isMainFrame, _ ->
                    val currentPage = view.url?.let(Uri::parse)
                    if (isMainFrame && isTrustedOrigin(origin, trustedOrigin) &&
                        currentPage?.let { OfflineBenches.isTrustedPage(it, benchKind) } == true
                    ) {
                        view.post {
                            runCatching {
                                val data = message.data.orEmpty()
                                if (JSONObject(data).optString("type") == "error") {
                                    receiver.reset()
                                    exportFailed = true
                                } else {
                                    val payload = receiver.accept(data)
                                    if (payload != null) {
                                        if (pendingExport != null) {
                                            exportFailed = true
                                        } else {
                                            pendingExport = payload
                                            val saveIntent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                                                addCategory(Intent.CATEGORY_OPENABLE)
                                                type = payload.mime
                                                putExtra(Intent.EXTRA_TITLE, payload.name)
                                            }
                                            runCatching { exportPicker.launch(saveIntent) }.onFailure {
                                                pendingExport = null
                                                exportFailed = true
                                            }
                                        }
                                    }
                                }
                            }.onFailure {
                                receiver.reset()
                                exportFailed = true
                            }
                        }
                    }
                }
                if (WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT)) {
                    WebViewCompat.addDocumentStartJavaScript(this, benchExportJavaScript, originRules)
                }
            }

            val supportsScanMessages = bench == Bench.CODE &&
                WebViewFeature.isFeatureSupported(WebViewFeature.WEB_MESSAGE_LISTENER)
            if (supportsScanMessages) {
                WebViewCompat.addWebMessageListener(
                    this, "KanarekScan", scanOrigins,
                ) { _, message, origin, isMainFrame, _ ->
                    if (isMainFrame && isTrustedOrigin(origin, trustedOrigin)) {
                        val scanned = runCatching {
                            val json = JSONObject(message.data.orEmpty())
                            val value = json.optString("text")
                            if (json.optInt("v") != 1 || value.isBlank() || value.length > 16384) null
                            else ScanPayloadParser.parse(value)
                        }.getOrNull()
                        scanned?.let(onScan)
                    }
                }
                if (WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT)) {
                    WebViewCompat.addDocumentStartJavaScript(this, codeScanObserver, scanOrigins)
                }
            }

            webViewClient = object : WebViewClient() {
                override fun shouldInterceptRequest(
                    view: WebView?, request: WebResourceRequest?,
                ): WebResourceResponse? =
                    if (assetLoader != null && request != null)
                        OfflineBenches.intercept(assetLoader, request.url, benchKind)
                    else null

                override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                    val target = request?.url ?: return true
                    if (trustedPage(target)) return false
                    if (request.isForMainFrame && target.scheme == "https") {
                        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, target)) }
                    }
                    return true
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    if (offline && url?.let { trustedPage(Uri.parse(it)) } == true &&
                        WebViewFeature.isFeatureSupported(WebViewFeature.WEB_MESSAGE_LISTENER) &&
                        !WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT)
                    ) {
                        view?.evaluateJavascript(benchExportJavaScript, null)
                    }
                    if (url?.let { trustedPage(Uri.parse(it)) } == true) {
                        startCameraIfRequested(view)
                        deliverFileIfReady(view)
                    }
                    if (supportsScanMessages &&
                        !WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT) &&
                        url?.let { trustedPage(Uri.parse(it)) } == true
                    ) {
                        view?.evaluateJavascript(codeScanObserver, null)
                    }
                }

                // shouldOverrideUrlLoading is not called for POST navigation.
                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                    val target = url?.let(Uri::parse)
                    if (target == null || !trustedPage(target)) view?.stopLoading()
                }
            }
            webChromeClient = object : WebChromeClient() {
                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                    if (newProgress == 100) deliverFileIfReady(view)
                }
                override fun onShowFileChooser(
                    webView: WebView?,
                    filePathCallback: ValueCallback<Array<Uri>>?,
                    fileChooserParams: FileChooserParams?,
                ): Boolean {
                    if (filePathCallback == null) return false
                    val currentPage = webView?.url?.let(Uri::parse)
                    if (fileCallback != null || currentPage == null ||
                        !trustedPage(currentPage)
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
            loadUrl(pageUrl)
        }
    }

    BackHandler {
        if (webView.canGoBack()) webView.goBack() else onExit()
    }
    LaunchedEffect(pendingScanRequest, webView) {
        startCameraIfRequested(webView)
    }
    LaunchedEffect(incomingFile?.id, webView) {
        deliverFileIfReady(webView)
    }
    DisposableEffect(webView, lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> webView.onPause()
                Lifecycle.Event.ON_RESUME -> webView.onResume()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            webView.onPause()
            fileCallback?.onReceiveValue(null)
            fileCallback = null
            cameraRequest?.deny()
            cameraRequest = null
            receiver.reset()
            pendingExport = null
        }
    }
    if (exportFailed) {
        AlertDialog(
            onDismissRequest = { exportFailed = false },
            title = { Text(stringResource(R.string.tools_export_failed_title)) },
            text = { Text(stringResource(R.string.tools_export_failed)) },
            confirmButton = {
                TextButton(onClick = { exportFailed = false }) { Text(stringResource(R.string.close)) }
            },
        )
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
