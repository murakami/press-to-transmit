package com.example.presstotransmit

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class MyBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "onReceive action: " + intent.action)
        Log.d(TAG, "onReceive extras: " + intent.extras)
    }

    companion object {
        private const val TAG = "MyBroadcastReceiver"
    }
}
