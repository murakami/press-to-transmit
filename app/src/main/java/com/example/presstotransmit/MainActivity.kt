package com.example.presstotransmit

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
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
    private val requestPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                // 権限が許可されたときの処理
                Toast.makeText(this, "音声録音権限が許可されました", Toast.LENGTH_SHORT).show()
            } else {
                // 権限が拒否されたときの処理
                Toast.makeText(this, "音声録音権限が必要です", Toast.LENGTH_LONG).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        checkAndRequestPermissions()
        /*
        VoicePing.init(this, "wss://router-lite.voiceping.info")
        VoicePing.connect("your_user_id", "your_company", object : ConnectCallback {
            override fun onConnected() {
                Log.d("MainActivity", "onConnected")
            }

            override fun onFailed(exception: VoicePingException) {
                Log.d("MainActivity", "onFailed")
            }
        })
         */
        enableEdgeToEdge()
        setContent {
            PressToTransmitTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    PressToTransmit(
                        //activity = MainActivity,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }

    private fun checkAndRequestPermissions() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED -> {
                // すでに権限が許可されている場合。必要であればログ出力などを行う。
            }
            // 必要であれば、なぜ権限が必要かを説明するUIをここに入れる (shouldShowRequestPermissionRationale)
            else -> {
                // 権限をリクエストする
                requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
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
fun PressToTransmit(
    //activity: MainActivity,
    modifier: Modifier = Modifier) {
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
            onClick = {
                VoicePing.startTalking(
                    receiverId = "efgh",
                    channelType = ChannelType.PRIVATE,
                    callback = context as OutgoingTalkCallback
                    //callback = null,
                    //destinationPath = null,
                    //recorder = null
                )
            }
        ) {
            Text("VoicePing.startTalking")
        }
        Button(
            onClick = {
                Log.d("MainActivity", "VoicePing: init & connect & startTalking: begin")
                val audioSource = AudioSourceConfig.getSource()
                val audioParam = AudioParam.Builder()
                    .setAudioSource(audioSource)
                    .build()
                val audioSourceText = AudioSourceConfig.getAudioSourceText(audioParam.audioSource)
                Log.d("MainActivity", "Manufacturer: ${Build.MANUFACTURER}, audio source: $audioSourceText")
                VoicePing.init(context, "wss://router-lite.voiceping.info", audioParam)
                VoicePing.connect("TUF-1", "bitz", object : ConnectCallback {
                    override fun onConnected() {
                        Log.d("MainActivity", "onConnected")
                        VoicePing.startTalking(
                            receiverId = "efgh",
                            channelType = ChannelType.PRIVATE,
                            callback = context as OutgoingTalkCallback
                        )
                    }

                    override fun onFailed(exception: VoicePingException) {
                        Log.d("MainActivity", "onFailed")
                    }
                })
                Log.d("MainActivity", "VoicePing: init & connect & startTalking: end")
            }
        ) {
            Text("VoicePing: init & connect & startTalking")
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