package com.example.sisonkeevents

import android.content.Context
import com.example.sisonkeevents.data.OfflineDatabaseHelper

class MyEventsManager(@Suppress("UNUSED_PARAMETER") context: Context) {

    private val db = OfflineDatabaseHelper.get()

    fun saveInterestedEvent(event: event) {
        db.saveInterestedEvent(event)
    }

    fun getInterestedEvents(): List<event> {
        return db.loadInterestedEvents()
    }
}