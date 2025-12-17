package com.phlox.tvwebbrowser.compose.runtime

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class ActivityBrowserPlatform(private val activity: ComponentActivity) : BrowserHost.Platform {

    private var host: BrowserHost? = null
    fun attachHost(host: BrowserHost) {
        this.host = host
    }

    fun detachHost() {
        host = null
    }

    private val _voiceUiState = MutableStateFlow(VoiceUiState())
    val voiceUiState = _voiceUiState.asStateFlow()

    private var speechRecognizer: SpeechRecognizer? = null
    private var listening = false

    private val fileChooserLauncher =
        activity.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            host?.deliverFileChooserResult(it.resultCode, it.data)
        }

    override fun launchFileChooser(intent: Intent): Boolean {
        fileChooserLauncher.launch(intent)
        return true
    }

    // ---- Permissions (ordered + requestCode-preserving) ----

    private data class PendingPermRequest(
        val requestCode: Int,
        val permissions: Array<String>,
    )

    private var pendingPermRequest: PendingPermRequest? = null

    private val permsLauncher =
        activity.registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { map ->
            val pending = pendingPermRequest ?: return@registerForActivityResult
            pendingPermRequest = null

            val grantResults =
                IntArray(pending.permissions.size) { i ->
                    val perm = pending.permissions[i]
                    if (map[perm] == true) PackageManager.PERMISSION_GRANTED else PackageManager.PERMISSION_DENIED
                }

            host?.deliverPermissionsResult(pending.requestCode, pending.permissions, grantResults)
        }

    override fun requestPermissions(requestCode: Int, permissions: Array<String>) {
        pendingPermRequest = PendingPermRequest(requestCode, permissions)
        permsLauncher.launch(permissions)
    }

    // ---- Voice ----

    private val voiceIntentLauncher =
        activity.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            val matches = it.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            host?.onVoiceQuery(matches?.firstOrNull())
        }

    override fun startVoiceSearch() {
        if (Build.VERSION.SDK_INT >= 30 && SpeechRecognizer.isRecognitionAvailable(activity)) {
            val sr = speechRecognizer ?: SpeechRecognizer.createSpeechRecognizer(activity).also { speechRecognizer = it }

            sr.setRecognitionListener(
                object : android.speech.RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {}
                    override fun onBeginningOfSpeech() {}
                    override fun onRmsChanged(rms: Float) {
                        _voiceUiState.value = _voiceUiState.value.copy(rmsDb = rms)
                    }
                    override fun onBufferReceived(buffer: ByteArray?) {}
                    override fun onEndOfSpeech() {}
                    override fun onError(error: Int) {
                        stopVoiceSearch()
                        toast("Voice error: $error")
                    }
                    override fun onResults(results: Bundle?) {
                        stopVoiceSearch()
                        host?.onVoiceQuery(results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull())
                    }
                    override fun onPartialResults(partialResults: Bundle?) {
                        val t = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty()
                        _voiceUiState.value = _voiceUiState.value.copy(partialText = t)
                    }
                    override fun onEvent(eventType: Int, params: Bundle?) {}
                }
            )

            _voiceUiState.value = VoiceUiState(active = true)
            listening = true

            sr.startListening(
                Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                }
            )
        } else {
            voiceIntentLauncher.launch(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH))
        }
    }

    fun stopVoiceSearch() {
        if (listening) {
            speechRecognizer?.stopListening()
            listening = false
        }
        _voiceUiState.value = VoiceUiState(active = false)
    }

    fun dispose() {
        speechRecognizer?.destroy()
        speechRecognizer = null
    }

    // ---- Platform helpers ----

    override fun copyToClipboard(text: String) {
        val cm = activity.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText("url", text))
        toast("Copied")
    }

    override fun shareText(text: String) {
        activity.startActivity(
            Intent.createChooser(
                Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, text)
                },
                null
            )
        )
    }

    override fun openExternal(url: String) {
        runCatching { activity.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
    }

    override fun toast(msg: String) {
        Toast.makeText(activity, msg, Toast.LENGTH_SHORT).show()
    }
}