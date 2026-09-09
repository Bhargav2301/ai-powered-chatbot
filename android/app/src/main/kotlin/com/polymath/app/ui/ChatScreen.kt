package com.polymath.app.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.polymath.app.FolioViewModel
import com.polymath.app.BuildConfig
import com.polymath.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray

@OptIn(ExperimentalLayoutApi::class)
@Composable fun ChatScreen(vm: FolioViewModel, close: () -> Unit) {
    val state by vm.ui.collectAsStateWithLifecycle()
    val scope by vm.chatScope.collectAsStateWithLifecycle()
    val busy by vm.chatBusy.collectAsStateWithLifecycle()
    val status by vm.chatStatus.collectAsStateWithLifecycle()
    val error by vm.chatError.collectAsStateWithLifecycle()
    val connectionMessage by vm.connectionMessage.collectAsStateWithLifecycle()
    val modelInstalled by vm.modelInstalled.collectAsStateWithLifecycle()
    val ready = if (state.settings.localAi) modelInstalled else state.settings.aiEnabled
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_STOP) vm.cancelChat() }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer); vm.cancelChat() }
    }
    var question by rememberSaveable { mutableStateOf("") }
    var connection by rememberSaveable { mutableStateOf(false) }
    var datasets by rememberSaveable { mutableStateOf(false) }
    val messages = state.folio.chat.filter { it.scope == scope }
    val listState = rememberLazyListState()
    LaunchedEffect(messages.size, busy) { if (messages.isNotEmpty()) listState.animateScrollToItem(messages.lastIndex) }
    Dialog(onDismissRequest = close, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        Surface(Modifier.fillMaxSize(), color = CanvasColor, contentColor = Ink) {
            Column(Modifier.safeDrawingPadding().imePadding().fillMaxSize().padding(horizontal = 18.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    IconButton(close) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Back to folio") }
                    TextButton({ connection = true }) { Text("AI settings") }
                    IconButton({ vm.clearChat() }) { Icon(Icons.Outlined.DeleteSweep, "Clear conversation") }
                }
                PageHeading("YOUR SOURCES, IN CONVERSATION", "Ask Polymath")
                Text("Polymath ${BuildConfig.VERSION_NAME}" + if (BuildConfig.APPLICATION_ID.endsWith(".offline")) " · Offline preview" else "", color = Muted, style = MaterialTheme.typography.labelSmall)
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(scope == "vault", { vm.selectScope("vault") }, label = { Text("My vault") })
                    state.folio.datasets.forEach { dataset -> FilterChip(scope == dataset.id, { vm.selectScope(dataset.id) }, label = { Text(dataset.title) }) }
                    AssistChip({ datasets = true }, label = { Text("Datasets") }, leadingIcon = { Icon(Icons.Outlined.LibraryAdd, null) })
                }
                if (!modelInstalled || !state.settings.localAi) Button({ connection = true }, Modifier.fillMaxWidth()) {
                    Text(if (state.settings.localAi) "Set up offline AI" else "Switch to offline AI")
                }
                Text(if (state.settings.localAi) (if (modelInstalled) "On-device AI · ready" else "On-device AI · one-time setup needed") + " · No API key."
                    else "Private server · only this scope is sent. Source images load from their original hosts.", color = Muted, style = MaterialTheme.typography.bodySmall)
                LazyColumn(Modifier.weight(1f).fillMaxWidth(), state = listState, contentPadding = PaddingValues(vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    items(messages, key = { it.id }) { message -> ChatBubble(message, state.settings.localAi) }
                    if (messages.isEmpty()) item { EmptyPanel("Start with something you keep.", "Ask a question about your saved sources and notes, or import a dataset. Every generated answer includes passages you can inspect.") }
                }
                if (busy) {
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(status, Modifier.weight(1f).padding(top = 12.dp), color = Sage)
                        TextButton({ vm.cancelChat() }) { Text("Stop") }
                    }
                }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error); TextButton({ vm.retryChat() }, enabled = !busy) { Text("Retry question") } }
                Row(Modifier.fillMaxWidth().padding(bottom = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(question, { question = it.take(if (state.settings.localAi) 800 else 2000) }, Modifier.weight(1f), label = { Text("Ask your selected sources") }, maxLines = 4, enabled = !busy)
                    IconButton({ vm.sendChat(question); question = "" }, enabled = !busy && question.isNotBlank() && ready,
                        modifier = Modifier.padding(top = 8.dp)) { Icon(Icons.AutoMirrored.Outlined.Send, "Send question", tint = if (ready) Sage else Muted) }
                }
            }
        }
    }
    if (connection) ConnectionDialog(vm, state.settings, connectionMessage, close = { connection = false }, save = { endpoint, key, enabled ->
        vm.configureAi(endpoint, key, enabled)
    })
    if (datasets) DatasetDialog(vm, state.folio, close = { datasets = false })
}

@Composable private fun ChatBubble(message: ChatMessageEntity, local: Boolean) {
    val uri = LocalUriHandler.current
    FolioPanel(Modifier.fillMaxWidth()) {
        Eyebrow(if (message.role == "user") "YOU" else if (message.status == "answered") "POLYMATH / QWEN · CHECK THE EVIDENCE" else "POLYMATH / EVIDENCE CHECK")
        Text(message.text, style = MaterialTheme.typography.bodyLarge)
        if (message.role == "assistant") {
            val citations = remember(message.citations) { JSONArray(message.citations) }
            for (i in 0 until citations.length()) {
                val citation = citations.getJSONObject(i)
                var expanded by rememberSaveable(message.id, i) { mutableStateOf(false) }
                OutlinedButton({ expanded = !expanded }, Modifier.fillMaxWidth()) { Text("[${citation.getString("id")}] ${citation.getString("title")}") }
                if (expanded) {
                    Text(citation.getString("excerpt"), color = Muted)
                    if (!local) SourceImages(Converters().imageList(citation.optJSONArray("images")?.toString() ?: "[]"))
                    if (!citation.isNull("source_url")) TextButton({ runCatching { uri.openUri(citation.getString("source_url")) } }) { Text("Open original source") }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable private fun ConnectionDialog(vm: FolioViewModel, settings: Settings, message: String?, close: () -> Unit, save: (String, String, Boolean) -> Unit) {
    var endpoint by rememberSaveable { mutableStateOf(settings.aiEndpoint) }
    var key by remember { mutableStateOf("") }
    var consent by rememberSaveable { mutableStateOf(settings.aiEnabled) }
    // Opening settings always shows local setup, including when a previous session used a server.
    var advancedServer by rememberSaveable { mutableStateOf(false) }
    ModalBottomSheet(close, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), containerColor = CanvasColor) {
        Column(Modifier.fillMaxWidth().heightIn(max = 640.dp).imePadding().verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(if (advancedServer) "Private server (advanced)" else "Offline AI", style = MaterialTheme.typography.headlineMedium)
            if (!advancedServer) {
                LocalModelControls(vm, settings.localAi)
                TextButton({ advancedServer = true }) { Text("Use a private server (advanced)") }
            } else {
            TextButton({ advancedServer = false }) { Text("Back to offline AI") }
            Text("Run the Polymath service on a computer or server you trust. It uses open-source MiniLM and Qwen models. Hosting and downloads are your responsibility.", color = Muted)
            OutlinedTextField(endpoint, { endpoint = it }, Modifier.fillMaxWidth(), label = { Text("HTTPS service origin") }, singleLine = true)
            OutlinedTextField(key, { key = it }, Modifier.fillMaxWidth(), label = { Text(if (settings.aiEnabled) "API key (blank keeps current)" else "Service API key") }, visualTransformation = PasswordVisualTransformation(), singleLine = true)
            Row { Checkbox(consent, { consent = it }); Text("Allow questions and text from my selected dataset to be sent to this service.", Modifier.padding(top = 8.dp)) }
            Text("The supplied service keeps no document database. Choose your host carefully. Disconnecting removes the stored API key.", color = Muted, style = MaterialTheme.typography.bodySmall)
            if (settings.aiEnabled) Text("Connected to ${settings.aiEndpoint}", color = Sage)
            message?.let { Text(it, color = Sage) }
            Button({ save(endpoint, key, consent); key = "" }, Modifier.fillMaxWidth(), enabled = consent || settings.aiEnabled) { Text(if (settings.aiEnabled && !consent) "Disconnect" else "Save connection") }
            }
            TextButton(close) { Text("Done") }
        }
    }
}

@Composable private fun LocalModelControls(vm: FolioViewModel, selected: Boolean) {
    val installed by vm.modelInstalled.collectAsStateWithLifecycle()
    val bundled by vm.modelBundled.collectAsStateWithLifecycle()
    val checked by vm.modelChecked.collectAsStateWithLifecycle()
    val busy by vm.modelBusy.collectAsStateWithLifecycle()
    val progress by vm.modelProgress.collectAsStateWithLifecycle()
    val message by vm.modelMessage.collectAsStateWithLifecycle()
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> if (uri != null) vm.installModel(uri) }
    Text("Qwen3 · 0.6B · 4-bit", style = MaterialTheme.typography.titleLarge)
    Text(if (bundled) "Qwen is included in this APK. Prepare it once, then chat offline. No download, account or API key."
        else "Download Qwen once (397 MB), then chat offline. No account or API key.", color = Muted)
    Text("Ask about your saved sources or an example dataset. A 64-bit device with about 4 GB RAM and enough free memory is required. Allow at least 460 MB of free storage for setup.", color = Muted, style = MaterialTheme.typography.bodySmall)
    if (!checked) {
        LinearProgressIndicator(Modifier.fillMaxWidth())
        Text("Checking included AI…")
    } else if (busy) {
        LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
        Text("Installing model · ${(progress * 100).toInt()}%")
        TextButton({ vm.cancelModelInstall() }) { Text("Stop installation") }
    } else if (installed) {
        Text("Installed · ready for offline chat", color = Sage)
        if (!selected) Button({ vm.useLocalAi(true) }, Modifier.fillMaxWidth()) { Text("Use offline AI") }
        TextButton({ vm.removeModel() }) { Text("Remove model to free storage") }
    } else {
        if (bundled) Button({ vm.installModel(bundled = true) }, Modifier.fillMaxWidth()) { Text("Prepare included AI") }
        else {
            Button({ vm.installModel() }, Modifier.fillMaxWidth()) { Text("Download Qwen · 397 MB") }
            OutlinedButton({ picker.launch(arrayOf("*/*")) }, Modifier.fillMaxWidth()) { Text("Import approved GGUF file") }
        }
    }
    message?.let { Text(it, color = Sage) }
}

@Composable private fun DatasetDialog(vm: FolioViewModel, snapshot: FolioSnapshot, close: () -> Unit) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var removing by remember { mutableStateOf<String?>(null) }
    val importMessage by vm.datasetMessage.collectAsStateWithLifecycle()
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) coroutineScope.launch {
            try {
                val json = withContext(Dispatchers.IO) { context.contentResolver.openInputStream(uri)?.use {
                    val output = java.io.ByteArrayOutputStream()
                    val buffer = ByteArray(8192)
                    while (output.size() <= DatasetParser.MAX_BYTES) {
                        val count = it.read(buffer, 0, minOf(buffer.size, DatasetParser.MAX_BYTES + 1 - output.size()))
                        if (count < 0) break
                        output.write(buffer, 0, count)
                    }
                    val bytes = output.toByteArray()
                    require(bytes.size <= DatasetParser.MAX_BYTES) { "Dataset exceeds 2 MiB." }
                    bytes.toString(Charsets.UTF_8)
                } ?: error("Could not open this file.") }
                vm.importDataset(json)
            } catch (e: Exception) { vm.reportError(e.message ?: "Import failed.") }
        }
    }
    AlertDialog(close, title = { Text("Knowledge datasets") }, text = {
        Column(Modifier.heightIn(max = 440.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Import a Polymath JSON dataset with source text, topic IDs, images, and optional recall questions. Imported sources join your discovery deck.")
            Text("Text stays on your phone until you query that dataset. Source images use their original HTTPS hosts.", color = Muted)
            if (snapshot.datasets.none { it.id == "polymath-foundations" }) OutlinedButton({
                coroutineScope.launch {
                    val json = withContext(Dispatchers.IO) { context.assets.open("foundations.json").bufferedReader().use { it.readText() } }
                    vm.importDataset(json)
                }
            }) { Text("Add example dataset") }
            importMessage?.let { Text(it, color = Sage) }
            snapshot.datasets.forEach { dataset ->
                HorizontalDivider(color = BorderColor)
                Text("${dataset.title} · ${snapshot.cards.count { it.datasetId == dataset.id }} sources")
                TextButton({ removing = dataset.id }) { Text("Remove dataset") }
            }
        }
    }, confirmButton = { TextButton({ picker.launch(arrayOf("application/json", "text/plain")) }) { Text("Import JSON") } },
        dismissButton = { TextButton(close) { Text("Done") } })
    removing?.let { id -> AlertDialog({ removing = null }, title = { Text("Remove this dataset?") },
        text = { Text("Its cards and saved copies will be removed. Chat history is cleared to remove source excerpts. Your own notes and EXP remain.") },
        confirmButton = { TextButton({ vm.deleteDataset(id); removing = null }) { Text("Remove") } },
        dismissButton = { TextButton({ removing = null }) { Text("Cancel") } }) }
}
