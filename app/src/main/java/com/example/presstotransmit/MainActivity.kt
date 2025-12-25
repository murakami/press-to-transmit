package com.example.presstotransmit

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.IntentFilter
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
//import androidx.compose.ui.input.pointer.consume
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkRequest
import com.example.presstotransmit.ui.theme.PressToTransmitTheme
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
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

    val myBroadcastReceiver = MyBroadcastReceiver()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        checkAndRequestPermissions()
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Create channel to show notifications.
            val channelId = getString(R.string.default_notification_channel_id)
            val channelName = getString(R.string.default_notification_channel_name)
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager?.createNotificationChannel(
                NotificationChannel(
                    channelId,
                    channelName,
                    NotificationManager.IMPORTANCE_HIGH,
                ),
            )
        }
        // Initialize Firebase
        FirebaseApp.initializeApp(this)
        askNotificationPermission()
        FirebaseMessaging.getInstance().token.addOnCompleteListener(OnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w("MainActivity", "Fetching FCM registration token failed", task.exception)
                return@OnCompleteListener
            }

            // Get new FCM registration token
            val token = task.result

            // Log and toast
            //val msg = getString(R.string.msg_token_fmt, token)
            Log.d("MainActivity", /*msg*/token)
            Toast.makeText(baseContext, token, Toast.LENGTH_SHORT).show()
        })

        /*
        val receiver = MyCancelReceiver()
        val filter = IntentFilter("com.example.presstotransmit.CANCEL")
        registerReceiver(receiver, filter)
         */

        val filter = IntentFilter()
        filter.addAction("android.intent.action.PTT.down")
        filter.addAction("android.intent.action.PTT.up")
        filter.addAction("android.intent.action.USER_PRESENT")
        filter.addAction("com.phonemax.intent.action.PTT")
        filter.addAction("com.phonemax.intent.action.PTT.down")
        filter.addAction("com.phonemax.intent.action.PTT.up")
        filter.addAction("cn.com.phonemax.intent.action.PTT")
        filter.addAction("cn.com.phonemax.intent.action.PTT.down")
        filter.addAction("cn.com.phonemax.intent.action.PTT.up")
        filter.addAction("android.media.VOLUME_CHANGED_ACTION")
        filter.addAction("android.intent.action.MAIN")
        filter.addCategory("android.intent.category.LAUNCHER")
        val listenToBroadcastsFromOtherApps = false
        val receiverFlags = if (listenToBroadcastsFromOtherApps) {
            ContextCompat.RECEIVER_EXPORTED
        } else {
            ContextCompat.RECEIVER_NOT_EXPORTED
        }
        ContextCompat.registerReceiver(this, myBroadcastReceiver, filter, receiverFlags)

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

    // Declare the launcher at the top of your Activity/Fragment:
    private val requestNotifyPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { isGranted: Boolean ->
        if (isGranted) {
            // FCM SDK (and your app) can post notifications.
        } else {
            // TODO: Inform user that that your app will not show notifications.
        }
    }

    private fun askNotificationPermission() {
        // This is only necessary for API level >= 33 (TIRAMISU)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
            ) {
                // FCM SDK (and your app) can post notifications.
            } else if (shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) {
                // TODO: display an educational UI explaining to the user the features that will be enabled
                //       by them granting the POST_NOTIFICATION permission. This UI should provide the user
                //       "OK" and "No thanks" buttons. If the user selects "OK," directly request the permission.
                //       If the user selects "No thanks," allow the user to continue without notifications.
            } else {
                // Directly ask for the permission
                requestNotifyPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
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
    val sharedPreferences = context.getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
    val clipboardManager = LocalClipboardManager.current
    var serverUrl by remember {
        mutableStateOf(sharedPreferences.getString("serverUrl", VoicePingWorker.SERVER_URL) ?: VoicePingWorker.SERVER_URL)
    }
    var company by remember {
        mutableStateOf(sharedPreferences.getString("company", "example") ?: "example")
    }
    var userId by remember {
        mutableStateOf(sharedPreferences.getString("userId", "user01") ?: "user01")
    }
    var receiverId by remember {
        mutableStateOf(sharedPreferences.getString("receiverId", "user02") ?: "user02")
    }
    var groupId by remember {
        mutableStateOf(sharedPreferences.getString("groupId", "group01") ?: "group01")
    }
    val textFieldModifier = Modifier.padding(1.dp)
    Column(modifier = modifier.padding(12.dp)) {
        Row {
            Text("Server URL:")
            TextField(value = serverUrl, onValueChange = { newUrl ->
                serverUrl = newUrl
                sharedPreferences.edit().putString("serverUrl", newUrl).apply()
            }, modifier = textFieldModifier)
        }
        Row {
            Text("Company:")
            TextField(value = company, onValueChange = { newCompany ->
                company = newCompany
                sharedPreferences.edit().putString("company", newCompany).apply()
            }, modifier = textFieldModifier)
        }
        Row {
            Text("User ID:")
            TextField(value = userId, onValueChange = { newUserId ->
                userId = newUserId
                sharedPreferences.edit().putString("userId", newUserId).apply()
            }, modifier = textFieldModifier)
        }
        Row {
            Text("Receiver ID:")
            TextField(value = receiverId, onValueChange = { newReceiverId ->
                receiverId = newReceiverId
                sharedPreferences.edit().putString("receiverId", newReceiverId).apply()
            }, modifier = textFieldModifier)
        }
        Row {
            Text("Group ID:")
            TextField(value = groupId, onValueChange = { newGroupId ->
                groupId = newGroupId
                sharedPreferences.edit().putString("groupId", newGroupId).apply()
            }, modifier = textFieldModifier)
        }
        Button(
            onClick = {
                Log.d("MainActivity", "Copy FCM token")
                FirebaseMessaging.getInstance().token.addOnCompleteListener(OnCompleteListener { task ->
                    if (!task.isSuccessful) {
                        Log.w(
                            "MainActivity",
                            "Fetching FCM registration token failed",
                            task.exception
                        )
                        return@OnCompleteListener
                    }

                    // Get new FCM registration token
                    val token = task.result

                    // Log
                    Log.d("MainActivity", token)
                    clipboardManager.setText(AnnotatedString(token))
                })
            }
        ) {
            Text("Copy FCM token")
        }
        /*
        Button(
            onClick = {
                Log.d("MainActivity", "VoicePingWorker.initVoicePing")
                val audioSource = AudioSourceConfig.getSource()
                val audioParam = AudioParam.Builder()
                    .setAudioSource(audioSource)
                    .build()
                val audioSourceText = AudioSourceConfig.getAudioSourceText(audioParam.audioSource)
                Log.d("MainActivity", "Manufacturer: ${Build.MANUFACTURER}, audio source: $audioSourceText")
                VoicePingWorker.disposeVoicePing()
                VoicePingWorker.initVoicePing(context, serverUrl, audioParam)
            }
        ) {
            Text("VoicePingWorker.initVoicePing")
        }
         */
        /*
        Button(
            onClick = {
                Log.d("MainActivity", "VoicePingWorker.connect")
                VoicePingWorker.connect(userId, company, object : ConnectCallback {
                    override fun onConnected() {
                        Log.d("MainActivity", "onConnected")
                    }

                    override fun onFailed(exception: VoicePingException) {
                        Log.d("MainActivity", "onFailed")
                    }
                })
            }
        ) {
            Text("VoicePingWorker.connect")
        }
         */
        Button(
            onClick = {
                Log.d("MainActivity", "create VoicePingWorker")
                val data = Data.Builder().apply {
                    putString("serverUrl", serverUrl)
                    putString("company", company)
                    putString("userId", userId)
                }.build()
                val voicePingWorkRequest: WorkRequest =
                    OneTimeWorkRequestBuilder<VoicePingWorker>()
                        .setInputData(data)
                        .addTag("VoicePing")
                        .build()
                WorkManager
                    .getInstance(context)
                    .enqueue(voicePingWorkRequest)
            }
        ) {
            Text("create VoicePingWorker")
        }
        /*
        Button(
            onClick = {
                Log.d("MainActivity", "VoicePingWorker.startTalking")
                VoicePingWorker.startTalking(
                    receiverId = receiverId,
                    channelType = ChannelType.PRIVATE,
                    callback = context as OutgoingTalkCallback
                    //callback = null,
                    //destinationPath = null,
                    //recorder = null
                )
            }
        ) {
            Text("VoicePingWorker.startTalking")
        }
        */
        /*
        Button(
            onClick = {
                Log.d("MainActivity", "VoicePing: init & connect & startTalking: begin")
                val audioSource = AudioSourceConfig.getSource()
                val audioParam = AudioParam.Builder()
                    .setAudioSource(audioSource)
                    .build()
                val audioSourceText = AudioSourceConfig.getAudioSourceText(audioParam.audioSource)
                Log.d("MainActivity", "Manufacturer: ${Build.MANUFACTURER}, audio source: $audioSourceText")
                Log.d("MainActivity", "call VoicePingWorker.init")
                VoicePingWorker.disposeVoicePing()
                VoicePingWorker.initVoicePing(context, VoicePingWorker.SERVER_URL, audioParam)
                Log.d("MainActivity", "call VoicePingWorker.connect")
                VoicePingWorker.connect("demo", "bitz", object : ConnectCallback {
                    override fun onConnected() {
                        Log.d("MainActivity", "onConnected")
                        Log.d("MainActivity", "call VoicePingWorker.startTalking")
                        VoicePingWorker.startTalking(
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
         */
        /*
        Button(
            onClick = {
                Log.d("MainActivity", "VoicePingWorker.stopTalking")
                VoicePingWorker.stopTalking()
            }
        ) {
            Text("VoicePingWorker.stopTalking")
        }
        */
        Button(
            onClick = {
                Log.d("MainActivity", "VoicePingWorker.stopTalking")
                VoicePingWorker.joinGroup(groupId)
            }
        ) {
            Text("VoicePingWorker.joinGroup")
        }
        Button(
            onClick = {
                Log.d("MainActivity", "VoicePingWorker.stopTalking")
                VoicePingWorker.leaveGroup(groupId)
            }
        ) {
            Text("VoicePingWorker.leaveGroup")
        }
        // PTT Button: BoxベースのカスタムボタンでpointerInputを使用
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(32.dp)
                .padding(horizontal = 8.dp, vertical = 4.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color(0xFF6200EE))
                .pointerInput(Unit) {
                    awaitEachGesture {
                        // ボタンが押されたとき
                        awaitFirstDown()
                        Log.d("MainActivity", "PTT Button Pressed - Starting talking")
                        VoicePingWorker.startTalking(
                            receiverId = receiverId,
                            channelType = ChannelType.PRIVATE,
                            callback = context as OutgoingTalkCallback
                            //callback = null,
                            //destinationPath = null,
                            //recorder = null
                        )

                        // ボタンが離されるまで待つ
                        val up = waitForUpOrCancellation()
                        if (up != null) {
                            Log.d("MainActivity", "PTT Button Released - Stopping talking")
                            VoicePingWorker.stopTalking()
                        }
                    }
                },
            //contentAlignment = Alignment.Center
        ) {
            Text(
                text = "PTT (Private)",
                color = Color.White
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(32.dp)
                .padding(horizontal = 8.dp, vertical = 4.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color(0xFF6200EE))
                .pointerInput(Unit) {
                    awaitEachGesture {
                        // ボタンが押されたとき
                        awaitFirstDown()
                        Log.d("MainActivity", "PTT Button Pressed - Starting talking")
                        VoicePingWorker.startTalking(
                            receiverId = groupId,
                            channelType = ChannelType.GROUP,
                            callback = context as OutgoingTalkCallback
                            //callback = null,
                            //destinationPath = null,
                            //recorder = null
                        )

                        // ボタンが離されるまで待つ
                        val up = waitForUpOrCancellation()
                        if (up != null) {
                            Log.d("MainActivity", "PTT Button Released - Stopping talking")
                            VoicePingWorker.stopTalking()
                        }
                    }
                },
            //contentAlignment = Alignment.Center
        ) {
            Text(
                text = "PTT (Group)",
                color = Color.White
            )
        }
        /*
        Button(
            onClick = {
                Log.d("MainActivity", "VoicePingWorker.disconnect")
                VoicePingWorker.disconnect(object : DisconnectCallback {
                    override fun onDisconnected() {
                        Log.d("MainActivity", "onDisconnected")
                    }
                })
            }
        ) {
            Text("VoicePingWorker.disconnect")
        }
         */
        Button(
            onClick = {
                Log.d("MainActivity", "dispose VoicePingWorker")
                VoicePingWorker.dispose(context)
            }
        ) {
            Text("dispose VoicePingWorker")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PressToTransmitPreview() {
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