package com.example.presstotransmit

import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkRequest
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.smartwalkie.voicepingsdk.VoicePing
import com.smartwalkie.voicepingsdk.callback.ConnectCallback
import com.smartwalkie.voicepingsdk.exception.VoicePingException
import com.smartwalkie.voicepingsdk.model.AudioParam
import com.smartwalkie.voicepingsdk.model.ChannelType

class MyFirebaseMessagingService : FirebaseMessagingService() {

    fun startTalking() {
        val audioSource = AudioSourceConfig.getSource()
        val audioParam = AudioParam.Builder()
            .setAudioSource(audioSource)
            .build()
        val audioSourceText = AudioSourceConfig.getAudioSourceText(audioParam.audioSource)
        Log.d("MyFirebaseMessagingService", "Manufacturer: ${Build.MANUFACTURER}, audio source: $audioSourceText")
        VoicePing.init(this, "wss://router-lite.voiceping.info", audioParam)
        VoicePing.connect("demo", "bitz", object : ConnectCallback {
            override fun onConnected() {
                Log.d("MyFirebaseMessagingService", "onConnected")
                VoicePing.startTalking(
                    receiverId = "efgh",
                    channelType = ChannelType.PRIVATE,
                    callback = null
                )
            }

            override fun onFailed(exception: VoicePingException) {
                Log.d("MainActivity", "onFailed")
            }
        })
    }

    override fun handleIntent(intent: Intent) {
        super.handleIntent(intent)
        if (intent.getExtras() == null) return

        Log.d(TAG, "handleIntent: extras: ${intent.getExtras()}")
        //startTalking()
        val uploadWorkRequest: WorkRequest =
            OneTimeWorkRequestBuilder<VoicePingWorker>()
                .build()
        WorkManager
            .getInstance(application)
            .enqueue(uploadWorkRequest)
    }

    /**
     * Called when message is received.
     *
     * @param remoteMessage Object representing the message received from Firebase Cloud Messaging.
     */
    // [START receive_message]
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Log.d(TAG, "onMessageReceived: From: ${remoteMessage.from}")

        if (remoteMessage.data.isNotEmpty()) {
            Log.d(TAG, "Message data payload: ${remoteMessage.data}")
        }

        // Check if message contains a notification payload.
        remoteMessage.notification?.let {
            Log.d(TAG, "Message Notification Body: ${it.body}")
        }

        //startTalking()
    }
    // [END receive_message]

    override fun onDeletedMessages() {
        Log.d(TAG, "onDeletedMessages")
        Log.d(TAG, "onDeletedMessages")
    }

    // [START on_new_token]
    /**
     * Called if the FCM registration token is updated. This may occur if the security of
     * the previous token had been compromised. Note that this is called when the
     * FCM registration token is initially generated so this is where you would retrieve the token.
     */
    override fun onNewToken(token: String) {
        Log.d(TAG, "onNewToken: Refreshed token: $token")
    }
    // [END on_new_token]

    companion object {
        private const val TAG = "MyFirebaseMsgService"
    }
}
