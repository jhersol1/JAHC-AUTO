package com.jahc.auto

import android.app.Application
import com.jahc.auto.data.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.get(this@App)
                val list = db.scheduleDao().getAllOnce()
                for (s in list) {
                    val digits = s.phone.filter { it.isDigit() }
                    if (digits.length == 9) {
                        val normalized = "51$digits"
                        db.scheduleDao().upsert(s.copy(phone = normalized))
                    }
                }
            } catch (_: Exception) {}
        }
    }
}
