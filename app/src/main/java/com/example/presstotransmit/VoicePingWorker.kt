package com.example.presstotransmit

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.HandlerThread
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.work.Worker
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.smartwalkie.voicepingsdk.VoicePing
import com.smartwalkie.voicepingsdk.callback.ConnectCallback
import com.smartwalkie.voicepingsdk.callback.DisconnectCallback
import com.smartwalkie.voicepingsdk.exception.VoicePingException
import com.smartwalkie.voicepingsdk.listener.AudioRecorder
import com.smartwalkie.voicepingsdk.listener.OutgoingTalkCallback
import com.smartwalkie.voicepingsdk.model.AudioParam
import com.smartwalkie.voicepingsdk.model.ChannelType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.internal.throwMissingFieldException

fun VoicePing.dispose() {
    val threadSet = Thread.getAllStackTraces().keys
    threadSet.forEach { thread ->
        if (thread.name == "VoicePingThread") {
            if (thread is HandlerThread) {
                val voicePingThread = thread as HandlerThread
                Log.d("VoicePing.dispose", voicePingThread.toString())
                voicePingThread.quitSafely()
            }
        }
    }
}

class VoicePingWorker (appContext: Context, workerParams: WorkerParameters): CoroutineWorker(appContext, workerParams), OutgoingTalkCallback {
    private val doneChannel = Channel<Boolean>()
    private val notificationManager =
        appContext.getSystemService(Context.NOTIFICATION_SERVICE) as
                NotificationManager

    override suspend fun doWork(): Result {
        Log.d(TAG, "doWork: ${Thread.currentThread().name}(${Thread.currentThread().id})")

        connect()
        //startTalking()
        setForeground(createForegroundInfo())
        runCatching {
            val done = doneChannel.receive()
        }.onFailure { throwable ->
            Log.d(TAG, "onFailure")
            //doneChannel.send(true)
            stopTalking()
        }
        /*
        runBlocking {
            val done = doneChannel.receive()
        }
         */
        stopTalking()

        // Indicate whether the work finished successfully with the Result
        return Result.success()
    }

    /*
    override fun onStopped() {
        super.onStopped()
        decline()
    }
     */

    fun decline() {
        Log.d(TAG, "decline")
        GlobalScope.launch {
            doneChannel.send(true)
        }
    }

    // Creates an instance of ForegroundInfo which can be used to update the
    // ongoing notification.
    private fun createForegroundInfo(): ForegroundInfo {
        Log.d(TAG, "createForegroundInfo")
        val channelId = applicationContext.getString(R.string.default_notification_channel_id)
        //val channelName = applicationContext.getString(R.string.default_notification_channel_name)
        //val id = applicationContext.getString(R.string.notification_channel_id)
        val title = "PTT"
        val cancel = "Cancel PTT"
        // This PendingIntent can be used to cancel the worker
        val intent = WorkManager.getInstance(applicationContext)
            .createCancelPendingIntent(getId())

        // Create a Notification channel if necessary
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createChannel()
        }

        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setContentTitle(title)
            .setTicker(title)
            .setContentText("PTT is running")
            .setSmallIcon(R.drawable.ic_stat_ic_notification)
            .setOngoing(true)
            // Add the cancel action to the notification which can
            // be used to cancel the worker
            .addAction(android.R.drawable.ic_delete, cancel, intent)
            .build()

        //return ForegroundInfo(NOTIFICATION_ID, notification)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(
                NOTIFICATION_ID,
                notification,
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            ForegroundInfo(NOTIFICATION_ID, notification)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createChannel() {
        // Create a Notification channel
        val channelId = applicationContext.getString(R.string.default_notification_channel_id)
        val channelName = applicationContext.getString(R.string.default_notification_channel_name)
        val notificationManager = applicationContext.getSystemService(NotificationManager::class.java)
        notificationManager?.createNotificationChannel(
            NotificationChannel(
                channelId,
                channelName,
                NotificationManager.IMPORTANCE_HIGH,
            ),
        )
    }

    private fun connect() {
        Log.d(TAG, "connect")
        val audioSource = AudioSourceConfig.getSource()
        val audioParam = AudioParam.Builder()
            .setAudioSource(audioSource)
            .build()
        val audioSourceText = AudioSourceConfig.getAudioSourceText(audioParam.audioSource)
        Log.d(TAG, "Manufacturer: ${Build.MANUFACTURER}, audio source: $audioSourceText")
        disposeVoicePing()
        initVoicePing(applicationContext, "wss://router-lite.voiceping.info", audioParam)
        connect("demo", "bitz", object : ConnectCallback {
            override fun onConnected() {
                Log.d(TAG, "onConnected")
            }

            override fun onFailed(exception: VoicePingException) {
                Log.d(TAG, "onFailed")
                disconnect(object : DisconnectCallback {
                    override fun onDisconnected() {
                        Log.d(TAG, "onDisconnected")
                    }
                })
            }
        })
    }

    private fun startTalking() {
        Log.d(TAG, "startTalking")
        val audioSource = AudioSourceConfig.getSource()
        val audioParam = AudioParam.Builder()
            .setAudioSource(audioSource)
            .build()
        val audioSourceText = AudioSourceConfig.getAudioSourceText(audioParam.audioSource)
        Log.d(TAG, "Manufacturer: ${Build.MANUFACTURER}, audio source: $audioSourceText")
        disposeVoicePing()
        initVoicePing(applicationContext, "wss://router-lite.voiceping.info", audioParam)
        connect("demo", "bitz", object : ConnectCallback {
            override fun onConnected() {
                Log.d(TAG, "onConnected")
                startTalking(
                    receiverId = "efgh",
                    channelType = ChannelType.PRIVATE,
                    callback = null
                )
            }

            override fun onFailed(exception: VoicePingException) {
                Log.d(TAG, "onFailed")
                disconnect(object : DisconnectCallback {
                    override fun onDisconnected() {
                        Log.d(TAG, "onDisconnected")
                    }
                })
            }
        })
    }

    private fun stopTalking() {
        Log.d(TAG, "stopTalking")
        stopTalking()
        disconnect(object : DisconnectCallback {
            override fun onDisconnected() {
                Log.d(TAG, "onDisconnected")
            }
        })
    }

    override fun onOutgoingTalkStarted(audioRecorder: AudioRecorder) {
        Log.d(TAG, "onOutgoingTalkStarted")
    }

    override fun onOutgoingTalkStopped(isTooShort: Boolean, isTooLong: Boolean) {
        Log.d(TAG, "onOutgoingTalkStopped")
    }

    override fun onDownloadUrlReceived(downloadUrl: String) {
        Log.d(TAG, "onDownloadUrlReceived")
    }

    override fun onOutgoingTalkError(e: VoicePingException) {
        Log.d(TAG, "onOutgoingTalkError")
    }

    companion object {
        private const val TAG = "VoicePingWorker"
        private const val NOTIFICATION_ID = 999
        //private val doneChannel = Channel<Boolean>()

        /*
        public fun decline() {
            Log.d(TAG, "decline")
            GlobalScope.launch {
                doneChannel.send(true)
            }
        }
         */

        fun initVoicePing(context: Context, serverUrl: String, audioParam: AudioParam) {
            GlobalScope.launch(Dispatchers.Main) {
                VoicePing.init(context, serverUrl, audioParam)
            }
        }

        fun disposeVoicePing() {
            GlobalScope.launch(Dispatchers.Main) {
                VoicePing.dispose()
            }
        }

        fun connect(userId: String, company: String, callback: ConnectCallback) {
            GlobalScope.launch(Dispatchers.Main) {
                VoicePing.connect(userId, company, callback)
            }
        }

        fun disconnect(callback: DisconnectCallback) {
            GlobalScope.launch(Dispatchers.Main) {
                VoicePing.disconnect(callback)
            }
        }

        fun startTalking(receiverId: String, channelType: Int, callback: OutgoingTalkCallback?) {
            GlobalScope.launch(Dispatchers.Main) {
                VoicePing.startTalking(receiverId, channelType, callback)
            }
        }

        fun stopTalking() {
            GlobalScope.launch(Dispatchers.Main) {
                VoicePing.stopTalking()
            }
        }

        fun joinGroup(groupId: String) {
            GlobalScope.launch(Dispatchers.Main) {
                VoicePing.joinGroup(groupId)
            }
        }

        fun leaveGroup(groupId: String) {
            GlobalScope.launch(Dispatchers.Main) {
                VoicePing.leaveGroup(groupId)
            }
        }

        fun mute(targetId: String, channelType: Int) {
            GlobalScope.launch(Dispatchers.Main) {
                VoicePing.mute(targetId, channelType)
            }
        }

        fun unmute(targetId: String, channelType: Int) {
            GlobalScope.launch(Dispatchers.Main) {
                VoicePing.unmute(targetId, channelType)
            }
        }
    }
}
