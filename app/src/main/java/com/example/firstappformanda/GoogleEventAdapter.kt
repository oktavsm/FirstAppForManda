package com.example.firstappformanda


import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class GoogleEventAdapter(private val eventList: List<GoogleCalendarEvent>) :
    RecyclerView.Adapter<GoogleEventAdapter.EventViewHolder>() {

    class EventViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val title: TextView = itemView.findViewById(R.id.tv_event_title)
        val time: TextView = itemView.findViewById(R.id.tv_event_time)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_google_event, parent, false)
        return EventViewHolder(view)
    }

    override fun onBindViewHolder(holder: EventViewHolder, position: Int) {
        val event = eventList[position]
        holder.title.text = event.title
        holder.time.text = if (event.endTime != null) {
            "${event.startTime} - ${event.endTime}"
        } else {
            event.startTime // Untuk event seharian
        }
    }

    override fun getItemCount(): Int = eventList.size
}
