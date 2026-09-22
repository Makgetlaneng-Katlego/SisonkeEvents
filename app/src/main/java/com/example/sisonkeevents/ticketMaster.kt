package com.example.sisonkeevents

import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.io.BufferedReader
import org.json.JSONObject
import java.time.LocalDate
import java.time.DayOfWeek
import java.time.temporal.TemporalAdjusters

class ticketMaster {
    val apiKey="lTzYJXAfyAKXD8Aq6JbRiZPjLOTco821"
    val apiUrl="https://app.ticketmaster.com/discovery/v2/"

    fun fetchEvents(categoryId: String): List<event> {
        val jsonString = builCategoriesURL(categoryId) ?: return emptyList()
        val events = mutableListOf<event>()
        try {
            val root = JSONObject(jsonString)
            val embedded = root.optJSONObject("_embedded") ?: return emptyList()
            val eventsArray = embedded.optJSONArray("events") ?: return emptyList()

            for (i in 0 until eventsArray.length()) {
                val eventObj = eventsArray.getJSONObject(i)
                val id = eventObj.optString("id")
                val name = eventObj.optString("name")
                
                val dates = eventObj.optJSONObject("dates")
                val start = dates?.optJSONObject("start")
                val localDate = start?.optString("localDate") ?: ""
                val localTime = start?.optString("localTime") ?: "19:00"
                
                val info = if (eventObj.has("info") && !eventObj.optString("info").isNullOrEmpty()) eventObj.getString("info") else "No description available."
                
                val classifications = eventObj.optJSONArray("classifications")
                val classification = classifications?.optJSONObject(0)
                val segment = classification?.optJSONObject("segment")
                val segmentName = segment?.optString("name") ?: "General"
                
                val ticketUrl = eventObj.optString("url") ?: ""
                
                val embeddedVenues = eventObj.optJSONObject("_embedded")
                val venues = embeddedVenues?.optJSONArray("venues")
                val venue = venues?.optJSONObject(0)
                val city = venue?.optJSONObject("city")?.optString("name") ?: ""
                
                val images = eventObj.optJSONArray("images")
                val imageUrl = images?.optJSONObject(0)?.optString("url")

                events.add(event(
                    id = id,
                    name = name,
                    date = localDate,
                    location = city,
                    imageUrl = imageUrl,
                    time = localTime,
                    description = info,
                    category = segmentName,
                    ticketUrl = ticketUrl,
                    age = "All Ages",
                    parking = "Available on-site"
                ))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return events
    }

    fun builCategoriesURL(categoryId:String):String?{
        var Respond:String?=null
        try{
            val url: URL = URI.create(apiUrl+"events.json?segmentId=$categoryId&apikey=${apiKey}").toURL()
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val reader: BufferedReader = BufferedReader(InputStreamReader(connection.inputStream))
                var line:String?
                val response = StringBuffer()
                while (reader.readLine().also {line=it}!=null){
                    response.append(line)
                }
                reader.close()
                Respond=response.toString()

            }else{
                println("Error: Unable to fetch data")
            }
        }catch(e:Exception){
            e.printStackTrace()
        }
        return Respond
    }

    fun fetchWeekendEvents(countryCode: String = "ZA"): List<event> {
        val today = LocalDate.now()
        val day = today.dayOfWeek
        val friday = when (day) {
            DayOfWeek.SATURDAY -> today.minusDays(1)
            DayOfWeek.SUNDAY -> today.minusDays(2)
            else -> today.with(TemporalAdjusters.nextOrSame(DayOfWeek.FRIDAY))
        }
        val sunday = friday.plusDays(2)

        val startDateTime = "${friday}T17:00:00Z"
        val endDateTime = "${sunday}T23:59:59Z"

        val urlString = "${apiUrl}events.json?countryCode=$countryCode&startDateTime=$startDateTime&endDateTime=$endDateTime&apikey=$apiKey"
        val events = mutableListOf<event>()
        try {
            val url: URL = URI.create(urlString).toURL()
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val reader = BufferedReader(InputStreamReader(connection.inputStream))
                var line: String?
                val response = StringBuffer()
                while (reader.readLine().also { line = it } != null) {
                    response.append(line)
                }
                reader.close()
                
                val jsonString = response.toString()
                val root = JSONObject(jsonString)
                val embedded = root.optJSONObject("_embedded") ?: return emptyList()
                val eventsArray = embedded.optJSONArray("events") ?: return emptyList()

                for (i in 0 until eventsArray.length()) {
                    val eventObj = eventsArray.getJSONObject(i)
                    val id = eventObj.optString("id")
                    val name = eventObj.optString("name")
                    
                    val dates = eventObj.optJSONObject("dates")
                    val start = dates?.optJSONObject("start")
                    val localDate = start?.optString("localDate") ?: ""
                    val localTime = start?.optString("localTime") ?: "19:00"
                    
                    val info = if (eventObj.has("info") && !eventObj.optString("info").isNullOrEmpty()) eventObj.getString("info") else "No description available."
                    
                    val classifications = eventObj.optJSONArray("classifications")
                    val classification = classifications?.optJSONObject(0)
                    val segment = classification?.optJSONObject("segment")
                    val segmentName = segment?.optString("name") ?: "General"
                    
                    val ticketUrl = eventObj.optString("url") ?: ""
                    
                    val embeddedVenues = eventObj.optJSONObject("_embedded")
                    val venues = embeddedVenues?.optJSONArray("venues")
                    val venue = venues?.optJSONObject(0)
                    val city = venue?.optJSONObject("city")?.optString("name") ?: ""
                    
                    val images = eventObj.optJSONArray("images")
                    val imageUrl = images?.optJSONObject(0)?.optString("url")

                    events.add(event(
                        id = id,
                        name = name,
                        date = localDate,
                        location = city,
                        imageUrl = imageUrl,
                        time = localTime,
                        description = info,
                        category = segmentName,
                        ticketUrl = ticketUrl,
                        age = "All Ages",
                        parking = "Available on-site"
                    ))
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return events
    }
}