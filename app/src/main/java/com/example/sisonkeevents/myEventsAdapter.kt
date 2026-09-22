package com.example.sisonkeevents

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class myEventsAdapter(
    private val events: List<event>,
    private val onItemClick: (event) -> Unit
) : RecyclerView.Adapter<myEventsAdapter.MyEventViewHolder>() {

    class MyEventViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.myEventTitle)
        val date: TextView = view.findViewById(R.id.myEventDate)
        val location: TextView = view.findViewById(R.id.myEventLocation)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyEventViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_my_event, parent, false)
        return MyEventViewHolder(view)
    }

    override fun onBindViewHolder(holder: MyEventViewHolder, position: Int) {
        val event = events[position]
        holder.title.text = event.name
        holder.date.text = event.date
        holder.location.text = event.location
        
        holder.itemView.setOnClickListener {
            onItemClick(event)
        }
    }

    override fun getItemCount() = events.size
}
