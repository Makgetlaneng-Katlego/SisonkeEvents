package com.example.sisonkeevents

data class event(
    val id: String,
    val name: String,
    val date: String,
    val location: String,
    val imageUrl: String?,
    val interestedCount: Int = (50..500).random(), // Simulated data
    val time: String = "19:00",
    val description: String = "No description available.",
    val category: String = "General",
    val ticketUrl: String = "",
    val age: String = "All Ages",
    val parking: String = "Available"
)

