package com.example.sisonkeevents.data

import com.example.sisonkeevents.event

interface EventApi {
    suspend fun fetchEventsForCategory(categoryId: String): List<event>
    suspend fun fetchWeekendEvents(countryCode: String): List<event>
    suspend fun markInterested(eventId: String)
}