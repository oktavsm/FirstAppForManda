package com.example.firstappformanda // <- SESUAIKAN

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class AgendaAdapter(private val agendaList: List<AgendaItem>) :
    RecyclerView.Adapter<AgendaAdapter.AgendaViewHolder>() {

    class AgendaViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val icon: ImageView = itemView.findViewById(R.id.iv_agenda_icon)
        val title: TextView = itemView.findViewById(R.id.tv_agenda_title)
        val subtitle: TextView = itemView.findViewById(R.id.tv_agenda_subtitle)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AgendaViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_agenda, parent, false)
        return AgendaViewHolder(view)
    }

    override fun onBindViewHolder(holder: AgendaViewHolder, position: Int) {
        val item = agendaList[position]
        holder.title.text = item.title
        holder.subtitle.text = item.subtitle

        // Ganti ikon berdasarkan tipe agenda
        if (item.type == "event") {
            holder.icon.setImageResource(R.drawable.ic_event) // Ganti dengan ikon kalender
        } else { // "task"
            holder.icon.setImageResource(R.drawable.ic_task) // Ganti dengan ikon checklist
        }

        // Di dalam onBindViewHolder, di paling bawah
        holder.itemView.startAnimation(AnimationUtils.loadAnimation(holder.itemView.context, R.anim.fade_in))
    }

    override fun getItemCount(): Int = agendaList.size
}
