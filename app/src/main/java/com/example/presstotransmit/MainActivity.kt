package com.example.presstotransmit

import android.media.audiofx.AcousticEchoCanceler
import android.media.audiofx.AutomaticGainControl
import android.media.audiofx.NoiseSuppressor
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.semantics.Role.Companion.Button
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.presstotransmit.ui.theme.PressToTransmitTheme
import com.smartwalkie.voicepingsdk.VoicePing
import com.smartwalkie.voicepingsdk.callback.ConnectCallback
import com.smartwalkie.voicepingsdk.callback.DisconnectCallback
import com.smartwalkie.voicepingsdk.exception.VoicePingException
import com.smartwalkie.voicepingsdk.listener.AudioRecorder
import com.smartwalkie.voicepingsdk.listener.OutgoingTalkCallback
import com.smartwalkie.voicepingsdk.model.AudioParam
import com.smartwalkie.voicepingsdk.model.ChannelType

class MainActivity : ComponentActivity(), OutgoingTalkCallback {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        VoicePing.init(this, "wss://router-lite.voiceping.info")
        VoicePing.connect("your_user_id", "your_company", object : ConnectCallback {
            override fun onConnected() {
                Log.d("MainActivity", "onConnected")
            }

            override fun onFailed(exception: VoicePingException) {
                Log.d("MainActivity", "onFailed")
            }
        })
        enableEdgeToEdge()
        setContent {
            PressToTransmitTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    PressToTransmit(
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }

    override fun onOutgoingTalkStarted(audioRecorder: AudioRecorder) {
        Log.d("MainActivity", "onOutgoingTalkStarted")
    }

    override fun onOutgoingTalkStopped(isTooShort: Boolean, isTooLong: Boolean) {
        Log.d("MainActivity", "onOutgoingTalkStopped")
    }

    override fun onDownloadUrlReceived(downloadUrl: String) {
        Log.d("MainActivity", "onDownloadUrlReceived")
    }

    override fun onOutgoingTalkError(e: VoicePingException) {
        Log.d("MainActivity", "onOutgoingTalkError")
    }
}

@Composable
fun PressToTransmit(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    Column(modifier = modifier.padding(12.dp)) {
        Button(
            onClick = {
                val audioSource = AudioSourceConfig.getSource()
                val audioParam = AudioParam.Builder()
                    .setAudioSource(audioSource)
                    .build()
                val audioSourceText = AudioSourceConfig.getAudioSourceText(audioParam.audioSource)
                Log.d("MainActivity", "Manufacturer: ${Build.MANUFACTURER}, audio source: $audioSourceText")
                VoicePing.init(context, "wss://router-lite.voiceping.info", audioParam)
            }
        ) {
            Text("VoicePing.init")
        }
        Button(
            onClick = {
                VoicePing.connect("TUF-1", "bitz", object : ConnectCallback {
                    override fun onConnected() {
                        Log.d("MainActivity", "onConnected")
                    }

                    override fun onFailed(exception: VoicePingException) {
                        Log.d("MainActivity", "onFailed")
                    }
                })
            }
        ) {
            Text("VoicePing.connect")
        }
        Button(
            onClick = { VoicePing.startTalking("efgh", ChannelType.PRIVATE, null/*context as OutgoingTalkCallback*/) }
        ) {
            Text("VoicePing.startTalking")
        }
        Button(
            onClick = { VoicePing.stopTalking() }
        ) {
            Text("VoicePing.stopTalking")
        }
        Button(
            onClick = {
                VoicePing.disconnect(object : DisconnectCallback {
                    override fun onDisconnected() {
                        Log.d("MainActivity", "onDisconnected")
                    }
                })
            }
        ) {
            Text("VoicePing.disconnect")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ressToTransmitPreview() {
    PressToTransmitTheme {
        PressToTransmit()
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    PressToTransmitTheme {
        Greeting("Android")
    }
}