package com.example.sisonkeevents

import android.app.Application
import com.example.sisonkeevents.data.EventRepository
import com.example.sisonkeevents.data.OfflineDatabaseHelper
import com.example.sisonkeevents.data.TicketMasterEventApi

class SisonkeApp : Application() {

    lateinit var repository: EventRepository
        private set

    override fun onCreate() {
        super.onCreate()


        OfflineDatabaseHelper.init(this)


        repository = EventRepository(this, TicketMasterEventApi())
    }
}