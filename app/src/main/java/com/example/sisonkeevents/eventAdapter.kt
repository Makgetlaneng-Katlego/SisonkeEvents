package com.example.sisonkeevents

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class eventAdapter(
    private val events: List<event>,
    private val onItemClick: (event) -> Unit
) : RecyclerView.Adapter<eventAdapter.EventViewHolder>() {

    class EventViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.eventTitle)
        val date: TextView = view.findViewById(R.id.eventDate)
        val location: TextView = view.findViewById(R.id.eventLocation)
        val interested: TextView = view.findViewById(R.id.interestedCount)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_event, parent, false)
        return EventViewHolder(view)
    }

    override fun onBindViewHolder(holder: EventViewHolder, position: Int) {
        val event = events[position]
        holder.title.text = event.name
        holder.date.text = event.date
        holder.location.text = event.location
        holder.interested.text = "${event.interestedCount} interested"
        
        holder.itemView.setOnClickListener {
            onItemClick(event)
        }
    }

    override fun getItemCount() = events.size
}
