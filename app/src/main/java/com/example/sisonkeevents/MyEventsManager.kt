package com.example.sisonkeevents

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

class MyEventsManager(context: Context) {
    private val prefs = context.getSharedPreferences("MyEventsPrefs", Context.MODE_PRIVATE)

    fun saveInterestedEvent(event: event) {
        val currentEvents = getInterestedEvents().toMutableList()
        if (currentEvents.none { it.id == event.id }) {
            currentEvents.add(event)
            saveEventsList(currentEvents)
        }
    }

    fun getInterestedEvents(): List<event> {
        val jsonString = prefs.getString("interested_events", null) ?: return emptyList()
        val list = mutableListOf<event>()
        try {
            val jsonArray = JSONArray(jsonString)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(
                    event(
                        id = obj.getString("id"),
                        name = obj.getString("name"),
                        date = obj.getString("date"),
                        location = obj.getString("location"),
                        imageUrl = if (obj.has("imageUrl") && !obj.isNull("imageUrl")) obj.getString("imageUrl") else null,
                        interestedCount = obj.optInt("interestedCount", 100),
                        time = obj.optString("time", "19:00"),
                        description = obj.optString("description", "No description available."),
                        category = obj.optString("category", "General"),
                        ticketUrl = obj.optString("ticketUrl", ""),
                        age = obj.optString("age", "All Ages"),
                        parking = obj.optString("parking", "Available")
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    private fun saveEventsList(events: List<event>) {
        val jsonArray = JSONArray()
        for (event in events) {
            val obj = JSONObject()
            obj.put("id", event.id)
            obj.put("name", event.name)
            obj.put("date", event.date)
            obj.put("location", event.location)
            obj.put("imageUrl", event.imageUrl)
            obj.put("interestedCount", event.interestedCount)
            obj.put("time", event.time)
            obj.put("description", event.description)
            obj.put("category", event.category)
            obj.put("ticketUrl", event.ticketUrl)
            obj.put("age", event.age)
            obj.put("parking", event.parking)
            jsonArray.put(obj)
        }
        prefs.edit().putString("interested_events", jsonArray.toString()).apply()
    }
}
