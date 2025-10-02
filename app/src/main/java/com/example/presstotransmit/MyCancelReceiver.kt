package com.example.presstotransmit

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log


class MyCancelReceiver: BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        Log.d(TAG, "c")
    }

    companion object {
        const val TAG = "MyCancelReceiver"
    }
}