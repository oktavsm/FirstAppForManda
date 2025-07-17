package com.example.firstappformanda // <- SESUAIKAN

import android.app.Activity
import android.content.Intent
import android.graphics.Paint
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ActivityAdapter(
    private var activityList: List<ActivityModel>,
    private val isReadOnly: Boolean = false // Tambahkan parameter isReadOnly
) : RecyclerView.Adapter<ActivityAdapter.ActivityViewHolder>() {

    class ActivityViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val title: TextView = itemView.findViewById(R.id.item_title)
        val subtitle: TextView = itemView.findViewById(R.id.item_subtitle)
        val checkbox: CheckBox = itemView.findViewById(R.id.item_checkbox)
        val icon: ImageView = itemView.findViewById(R.id.item_icon) // Tambahkan ImageView untuk ikon
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ActivityViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_activity, parent, false)
        return ActivityViewHolder(view)
    }

    override fun onBindViewHolder(holder: ActivityViewHolder, position: Int) {
        val activity = activityList[position]
        holder.title.text = activity.title

        activity.startTime?.let {
            holder.subtitle.visibility = View.VISIBLE
            holder.subtitle.text = "Mulai: ${formatTimestamp(it, "dd MMM, HH:mm")}"
        } ?: run {
            holder.subtitle.visibility = View.GONE
        }

        if (activity.type == "task") {
            holder.checkbox.visibility = View.VISIBLE
            holder.icon.visibility = View.GONE // Sembunyikan ikon jika task
            // (DIPERBAIKI) Cek dengan aman, jika null anggap false
            val isChecked = activity.isDone == true
            holder.checkbox.isChecked = isChecked
            updateStrikeThrough(holder.title, isChecked)
            // Hanya set listener jika tidak read-only
            if (!isReadOnly) {
                holder.checkbox.setOnCheckedChangeListener(null)
                holder.checkbox.setOnCheckedChangeListener { _, newCheckedState ->
                    updateTaskStatus(activity.id, newCheckedState)
                }
            } else {
                holder.checkbox.isEnabled = false // Disable checkbox jika read-only
            }
        } else { // "event"
            holder.checkbox.visibility = View.GONE
            holder.icon.visibility = View.VISIBLE // Tampilkan ikon jika event
            holder.icon.setImageResource(R.drawable.ic_event) // Set ikon event
            updateStrikeThrough(holder.title, false)
        }

        holder.itemView.setOnClickListener {
            // Hanya buka detail jika tidak read-only
            if (!isReadOnly) {
                val context = holder.itemView.context
                val intent = Intent(context, ActivityDetailActivity::class.java)
                intent.putExtra("ACTIVITY_ID", activity.id)
                intent.putExtra("ACTIVITY_TYPE", activity.type)
                context.startActivity(intent)
                if (context is Activity) {
                    context.overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                }
            } // Jika read-only, klik tidak melakukan apa-apa
        }
    }

    override fun getItemCount(): Int = activityList.size

    private fun formatTimestamp(timestamp: Long, format: String): String { return try {
        SimpleDateFormat(format, Locale.getDefault()).format(Date(timestamp))
    } catch (e: Exception) { "Invalid Date" } }

    private fun updateTaskStatus(activityId: String?, isDone: Boolean) {
        if (activityId == null) return
        FirebaseFirestore.getInstance().collection("activities").document(activityId)
            .update("isDone", isDone)
            .addOnFailureListener { e ->
                Log.e("ActivityAdapter", "Error updating status", e)
            }
    }

    private fun updateStrikeThrough(textView: TextView, isDone: Boolean) {  if (isDone) {
        textView.paintFlags = textView.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
    } else {
        textView.paintFlags = textView.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
    } }
}
