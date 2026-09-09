package com.jahc.auto.worker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.jahc.auto.data.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiverAuto : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action in listOf(Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_LOCKED_BOOT_COMPLETED, "android.intent.action.QUICKBOOT_POWERON")) {
            CoroutineScope(Dispatchers.IO).launch {
                val db = AppDatabase.get(context)
                val list = db.scheduleDao().getEnabled()
                list.forEach { ScheduleWorker.scheduleNext(context, it) }
            }
        }
    }
}
