package org.mlm.browkorftv.utils

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.net.toUri
import org.mlm.browkorftv.R

class VoiceSearchHelper(
    private val activity: ComponentActivity,
    private val showMessage: (String) -> Unit = {}
) {

    private var isListening by mutableStateOf(false)
    private var partialResult by mutableStateOf("")
    private var rmsDb by mutableFloatStateOf(0f)

    private lateinit var callback: Callback
    private var languageModel: String = RecognizerIntent.LANGUAGE_MODEL_WEB_SEARCH

    private val legacyVoiceLauncher: ActivityResultLauncher<Intent> =
        activity.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == android.app.Activity.RESULT_OK) {
                val matches = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                callback.onResult(matches?.firstOrNull())
            }
        }

    private val permissionLauncher: ActivityResultLauncher<String> =
        activity.registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    startRecognitionAndroid11Plus()
                }
            } else {
                showMessage("Microphone permission required")
            }
        }

    private var speechRecognizer: SpeechRecognizer? = null

    interface Callback {
        fun onResult(text: String?)
    }

    fun initiateVoiceSearch(callback: Callback,
                            languageModel: String = RecognizerIntent.LANGUAGE_MODEL_WEB_SEARCH) {
        this.callback = callback
        this.languageModel = languageModel

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            checkPermissionAndStart()
        } else {
            startLegacyVoiceIntent()
        }
    }

    private fun startLegacyVoiceIntent() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, languageModel)
            putExtra(RecognizerIntent.EXTRA_PROMPT, activity.getString(R.string.speak))
        }

        if (intent.resolveActivity(activity.packageManager) != null) {
            try {
                legacyVoiceLauncher.launch(intent)
            } catch (e : Exception) {
                Log.e("VoiceSearchHelper", "Failed to start voice recognition", e)
                showMessage(activity.getString(R.string.error))
            }
        } else {
            showInstallVoiceEnginePrompt()
        }
    }

    private fun checkPermissionAndStart() {
        if (activity.checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                startRecognitionAndroid11Plus()
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun startRecognitionAndroid11Plus() {
        // Reset State
        isListening = true
        partialResult = ""
        rmsDb = 0f

        activity.runOnUiThread {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(activity).apply {
                setRecognitionListener(createRecognitionListener())
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, languageModel)
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, activity.packageName)
                }
                startListening(intent)
            }
        }
    }

    private fun createRecognitionListener() = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {}
        override fun onBeginningOfSpeech() {}
        override fun onRmsChanged(rmsdB: Float) {
            this@VoiceSearchHelper.rmsDb = rmsdB
        }
        override fun onBufferReceived(buffer: ByteArray?) {}
        override fun onEndOfSpeech() {}
        override fun onError(error: Int) {
            val message = when (error) {
                SpeechRecognizer.ERROR_NO_MATCH -> "No speech recognized. Please try again."
                SpeechRecognizer.ERROR_NETWORK -> "Network error. Check connection."
                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission required."
                else -> "Voice search error: $error"
            }
            stopListening()
            showMessage(activity.getString(R.string.error))
            Log.e("VoiceSearchHelper","Error: $message")
        }
        override fun onResults(results: Bundle?) {
            stopListening()
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            callback.onResult(matches?.firstOrNull())
        }
        override fun onPartialResults(partialResults: Bundle?) {
            val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            if (!matches.isNullOrEmpty()) {
                partialResult = matches.first()
            }
        }
        override fun onEvent(eventType: Int, params: Bundle?) {}
    }

    private fun stopListening() {
        isListening = false
        activity.runOnUiThread {
            speechRecognizer?.destroy()
            speechRecognizer = null
        }
    }

    private fun showInstallVoiceEnginePrompt() {
        val dialogBuilder = AlertDialog.Builder(activity)
            .setTitle(R.string.app_name)
            .setMessage(R.string.voice_search_not_found)
            .setNeutralButton(android.R.string.ok) { _, _ -> }

        val appPackageName = if (Utils.isTV(activity)) "com.google.android.katniss" else "com.google.android.googlequicksearchbox"
        val intent = Intent(Intent.ACTION_VIEW, "market://details?id=$appPackageName".toUri())

        if (activity.packageManager.queryIntentActivities(intent, 0).isNotEmpty()) {
            dialogBuilder.setPositiveButton(R.string.find_in_apps_store) { _, _ ->
                try {
                    activity.startActivity(intent)
                } catch (e: Exception) {
                    e.printStackTrace()
                    showMessage(activity.getString(R.string.error))
                }
            }
        }
        dialogBuilder.show()
    }

    /**
     * It will only render when isListening is true.
     */
    @Composable
    fun VoiceSearchUI() {
        if (!isListening) return

        Dialog(
            onDismissRequest = {
                speechRecognizer?.stopListening()
                stopListening()
            },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.6f)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .shadow(8.dp, RoundedCornerShape(12.dp))
                        .background(Color(0xFF222222), RoundedCornerShape(12.dp)) // Dark theme background
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Animated Mic
                    MicrophoneVisualizer(rmsDb = rmsDb)

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = partialResult.ifEmpty { stringResource(R.string.speak) },
                        color = Color.White,
                        style = MaterialTheme.typography.headlineSmall,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }

    @Composable
    private fun MicrophoneVisualizer(rmsDb: Float) {
        val infiniteTransition = rememberInfiniteTransition(label = "mic_pulse")

        // Bounce Animation
        val scale by infiniteTransition.animateFloat(
            initialValue = 1.0f,
            targetValue = 1.15f,
            animationSpec = infiniteRepeatable(
                animation = tween(1000, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ), label = "scale"
        )

        // Color Logic based on RMS (approx -2 to 10dB)
        // Original logic: 0f, 0.4f * frac, 0.8f * frac
        val minRms = -2f
        val maxRms = 10f
        val fraction = ((rmsDb - minRms) / (maxRms - minRms)).coerceIn(0f, 1f)

        // Interpolate color from White (silent) to Blueish (loud)
        val targetColor = Color(
            red = 0f,
            green = 0.4f * fraction,
            blue = 0.8f * fraction,
            alpha = 1f
        )
        // If low volume, keep it white/grey, else use the dynamic color
        val displayColor by animateColorAsState(
            targetValue = if (fraction < 0.2f) Color.White else targetColor,
            label = "color"
        )

        Icon(
            imageVector = ImageVector.vectorResource(R.drawable.outline_mic_24),
            contentDescription = "Microphone",
            tint = displayColor,
            modifier = Modifier
                .size(72.dp)
                .scale(scale)
        )
    }
}