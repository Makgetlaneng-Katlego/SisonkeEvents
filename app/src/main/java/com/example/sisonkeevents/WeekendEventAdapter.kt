package com.example.sisonkeevents

import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URL

class WeekendEventAdapter(
    private val events: List<event>,
    private val scope: CoroutineScope,
    private val onItemClick: (event) -> Unit
) : RecyclerView.Adapter<WeekendEventAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val image: ImageView = view.findViewById(R.id.eventImage)
        val title: TextView = view.findViewById(R.id.eventTitle)
        val date: TextView = view.findViewById(R.id.eventDate)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_weekend_event, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val event = events[position]
        holder.title.text = event.name
        holder.date.text = event.date
        holder.image.setImageResource(R.drawable.ic_calendar)

        if (!event.imageUrl.isNullOrEmpty()) {
            scope.launch(Dispatchers.IO) {
                try {
                    val url = URL(event.imageUrl)
                    val bitmap = BitmapFactory.decodeStream(url.openStream())
                    withContext(Dispatchers.Main) {
                        holder.image.setImageBitmap(bitmap)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        holder.itemView.setOnClickListener {
            onItemClick(event)
        }
    }

    override fun getItemCount() = events.size
}