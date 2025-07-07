// Jangan lupa sesuaikan nama paketmu
package com.example.firstappformanda

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class TaskAdapter(private val taskList: List<Task>) : RecyclerView.Adapter<TaskAdapter.TaskViewHolder>() {

    // Ini membuat "slot" atau tampilan untuk setiap baris
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(android.R.layout.simple_list_item_1, parent, false)
        return TaskViewHolder(view)
    }

    // Ini memasukkan data ke dalam setiap slot
    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        val task = taskList[position]
        holder.taskTitle.text = task.title
    }

    // Ini memberitahu RecyclerView berapa banyak total item yang ada
    override fun getItemCount(): Int {
        return taskList.size
    }

    // Ini adalah representasi dari setiap baris di layout
    class TaskViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val taskTitle: TextView = itemView.findViewById(android.R.id.text1)
    }
}