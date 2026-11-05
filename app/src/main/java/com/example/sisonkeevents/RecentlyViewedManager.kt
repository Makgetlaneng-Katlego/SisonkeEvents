package com.example.sisonkeevents

import android.content.Context
import com.example.sisonkeevents.data.OfflineDatabaseHelper

class RecentlyViewedManager(@Suppress("UNUSED_PARAMETER") context: Context) {
    private val db = OfflineDatabaseHelper.get()
    fun saveViewedEvent(event: event) {
        db.saveViewedEvent(event)
    }
    fun getViewedEvents(): List<event> {
        return db.loadRecentlyViewedEvents()
    }
}