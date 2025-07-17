package com.example.firstappformanda // <- SESUAIKAN

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class MyActivitiesFragment : Fragment() {

    // --- Deklarasi Variabel ---
    private lateinit var rvEvents: RecyclerView
    private lateinit var rvTasks: RecyclerView
    private lateinit var fabAddActivity: FloatingActionButton

    private lateinit var eventsAdapter: ActivityAdapter
    private lateinit var tasksAdapter: ActivityAdapter

    private val eventList = mutableListOf<ActivityModel>()
    private val taskList = mutableListOf<ActivityModel>()

    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_my_activities, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Inisialisasi
        firestore = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()
        initializeViews(view)

        // Setup Adapters
        eventsAdapter = ActivityAdapter(eventList)
        tasksAdapter = ActivityAdapter(taskList)
        rvEvents.layoutManager = LinearLayoutManager(requireContext())
        rvTasks.layoutManager = LinearLayoutManager(requireContext())
        rvEvents.adapter = eventsAdapter
        rvTasks.adapter = tasksAdapter

        // Muat kedua jenis data
        loadEvents()
        loadTasks()

        // Atur listener untuk tombol FAB
        fabAddActivity.setOnClickListener {
            showAddActivityDialog()

        }
    }

    private fun initializeViews(view: View) {
        rvEvents = view.findViewById(R.id.rv_my_events)
        rvTasks = view.findViewById(R.id.rv_my_tasks)
        fabAddActivity = view.findViewById(R.id.fab_add_activity)
    }

    private fun showAddActivityDialog() {
        val options = arrayOf("Tambah Jadwal Baru", "Tambah Tugas Baru")
        AlertDialog.Builder(requireContext())
            .setTitle("Pilih Jenis Aktivitas")
            .setItems(options) { _, which ->
                val activityType = if (which == 0) "event" else "task"
                val intent = Intent(requireContext(), ActivityDetailActivity::class.java)
                intent.putExtra("IS_READ_ONLY", false) // Tambahkan ini untuk menandakan mode edit/buat baru
                intent.putExtra("ACTIVITY_TYPE", activityType)
                startActivity(intent)
                requireActivity().overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out) // <-- TAMBAHKAN DI SINI
            }
            .show()
    }

    private fun loadEvents() {
        val userId = auth.currentUser?.uid ?: return
        firestore.collection("activities")
            .whereEqualTo("ownerId", userId)
            .whereEqualTo("type", "event")
            .orderBy("startTime", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshots, e ->
                if (e != null) { Log.w("MyActivities", "Event listen failed.", e); return@addSnapshotListener }
                if (snapshots != null) {
                    eventList.clear()
                    for (doc in snapshots) {
                        eventList.add(doc.toObject(ActivityModel::class.java).copy(id = doc.id))
                    }
                    eventsAdapter.notifyDataSetChanged()
                }
            }
    }

    private fun loadTasks() {
        val userId = auth.currentUser?.uid ?: return
        firestore.collection("activities")
            .whereEqualTo("ownerId", userId)
            .whereEqualTo("type", "task")
            .orderBy("endTime", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshots, e ->
                if (e != null) { Log.w("MyActivities", "Task listen failed.", e); return@addSnapshotListener }
                if (snapshots != null) {
                    taskList.clear()
                    for (doc in snapshots) {
                        taskList.add(doc.toObject(ActivityModel::class.java).copy(id = doc.id))
                    }
                    tasksAdapter.notifyDataSetChanged()
                }
            }
    }
}
