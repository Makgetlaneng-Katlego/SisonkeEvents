package com.example.sisonkeevents.data

import com.example.sisonkeevents.event
import com.example.sisonkeevents.ticketMaster
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class TicketMasterEventApi(
    private val client: ticketMaster = ticketMaster()
) : EventApi {

    override suspend fun fetchEventsForCategory(categoryId: String): List<event> =
        withContext(Dispatchers.IO) { client.fetchEvents(categoryId) }

    override suspend fun fetchWeekendEvents(countryCode: String): List<event> =
        withContext(Dispatchers.IO) { client.fetchWeekendEvents(countryCode) }

    override suspend fun markInterested(eventId: String) {

    }
}