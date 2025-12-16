package com.phlox.tvwebbrowser.compose.runtime

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.os.Build
import android.os.Bundle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class ActivityBrowserPlatform(private val activity: ComponentActivity) : BrowserHost.Platform {
    private var host: BrowserHost? = null
    fun attachHost(host: BrowserHost) { this.host = host }

    private val _voiceUiState = MutableStateFlow(VoiceUiState())
    val voiceUiState = _voiceUiState.asStateFlow()
    private var speechRecognizer: SpeechRecognizer? = null
    private var listening = false

    private val fileChooserLauncher = activity.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        host?.deliverFileChooserResult(it.resultCode, it.data)
    }
    override fun launchFileChooser(intent: Intent): Boolean { fileChooserLauncher.launch(intent); return true }

    private val permsLauncher = activity.registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { map ->
        val grant = IntArray(map.size) { i -> if(map.values.elementAt(i)) 0 else -1 }
        host?.deliverPermissionsResult(1001, map.keys.toTypedArray(), grant)
    }
    override fun requestPermissions(requestCode: Int, permissions: Array<String>) { permsLauncher.launch(permissions) }

    private val voiceIntentLauncher = activity.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        val matches = it.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
        host?.onVoiceQuery(matches?.firstOrNull())
    }
    override fun startVoiceSearch() {
        if (Build.VERSION.SDK_INT >= 30 && SpeechRecognizer.isRecognitionAvailable(activity)) {
            val sr = speechRecognizer ?: SpeechRecognizer.createSpeechRecognizer(activity).also { speechRecognizer = it }
            sr.setRecognitionListener(object : android.speech.RecognitionListener {
                override fun onReadyForSpeech(p: Bundle?) {}
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(r: Float) { _voiceUiState.value = _voiceUiState.value.copy(rmsDb = r) }
                override fun onBufferReceived(b: ByteArray?) {}
                override fun onEndOfSpeech() {}
                override fun onError(e: Int) { stopVoiceSearch(); toast("Voice Error $e") }
                override fun onResults(b: Bundle?) { 
                    stopVoiceSearch()
                    host?.onVoiceQuery(b?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull())
                }
                override fun onPartialResults(b: Bundle?) {
                    val t = b?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull() ?: ""
                    _voiceUiState.value = _voiceUiState.value.copy(partialText = t)
                }
                override fun onEvent(e: Int, p: Bundle?) {}
            })
            _voiceUiState.value = VoiceUiState(active = true)
            listening = true
            sr.startListening(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply { 
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            })
        } else {
            voiceIntentLauncher.launch(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH))
        }
    }
    fun stopVoiceSearch() {
        if(listening) { speechRecognizer?.stopListening(); listening = false }
        _voiceUiState.value = VoiceUiState(active = false)
    }
    fun dispose() { speechRecognizer?.destroy() }

    override fun copyToClipboard(text: String) {
        (activity.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText("url", text))
        toast("Copied")
    }
    override fun shareText(text: String) = activity.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply { type="text/plain"; putExtra(Intent.EXTRA_TEXT, text) }, null))
    override fun openExternal(url: String) { runCatching { activity.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) } }
    override fun toast(msg: String) = Toast.makeText(activity, msg, Toast.LENGTH_SHORT).show()
}