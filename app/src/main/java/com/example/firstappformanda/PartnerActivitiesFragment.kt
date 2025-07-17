package com.example.firstappformanda // <- SESUAIKAN

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class PartnerActivitiesFragment : Fragment() {

    // --- Deklarasi Variabel ---
    private lateinit var rvPartnerEvents: RecyclerView
    private lateinit var rvPartnerTasks: RecyclerView

    private lateinit var partnerEventsAdapter: ActivityAdapter
    private lateinit var partnerTasksAdapter: ActivityAdapter

    private val partnerEventList = mutableListOf<ActivityModel>()
    private val partnerTaskList = mutableListOf<ActivityModel>()

    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_partner_activities, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Inisialisasi
        firestore = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()
        initializeViews(view)

        // Setup Adapters
        partnerEventsAdapter = ActivityAdapter(partnerEventList, isReadOnly = true) // Set read-only
        partnerTasksAdapter = ActivityAdapter(partnerTaskList, isReadOnly = true) // Set read-only
        rvPartnerEvents.layoutManager = LinearLayoutManager(requireContext())
        rvPartnerTasks.layoutManager = LinearLayoutManager(requireContext())
        rvPartnerEvents.adapter = partnerEventsAdapter
        rvPartnerTasks.adapter = partnerTasksAdapter

        // Mulai proses memuat data pasangan
        loadPartnerIdAndFetchActivities()
    }

    private fun initializeViews(view: View) {
        rvPartnerEvents = view.findViewById(R.id.rv_partner_events)
        rvPartnerTasks = view.findViewById(R.id.rv_partner_tasks)
    }

    private fun loadPartnerIdAndFetchActivities() {
        val myUserId = auth.currentUser?.uid ?: return
        firestore.collection("users").document(myUserId).get()
            .addOnSuccessListener { document ->
                val partnerId = document.getString("coupleId")
                if (partnerId.isNullOrEmpty()) {
                    // Handle jika belum terhubung
                } else {
                    // Jika sudah terhubung, muat jadwal dan tugasnya
                    loadPartnerEvents(partnerId)
                    loadPartnerTasks(partnerId)
                }
            }
    }

    private fun loadPartnerEvents(partnerId: String) {
        firestore.collection("activities")
            .whereEqualTo("ownerId", partnerId)
            .whereEqualTo("type", "event")
            .orderBy("startTime", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshots, e ->
                if (e != null) { Log.w("PartnerActivities", "Event listen failed.", e); return@addSnapshotListener }
                if (snapshots != null) {
                    partnerEventList.clear()
                    for (doc in snapshots) {
                        partnerEventList.add(doc.toObject(ActivityModel::class.java).copy(id = doc.id))
                    }
                    partnerEventsAdapter.notifyDataSetChanged()
                }
            }
    }

    private fun loadPartnerTasks(partnerId: String) {
        firestore.collection("activities")
            .whereEqualTo("ownerId", partnerId)
            .whereEqualTo("type", "task")
            .orderBy("endTime", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshots, e ->
                if (e != null) { Log.w("PartnerActivities", "Task listen failed.", e); return@addSnapshotListener }
                if (snapshots != null) {
                    partnerTaskList.clear()
                    for (doc in snapshots) {
                        partnerTaskList.add(doc.toObject(ActivityModel::class.java).copy(id = doc.id))
                    }
                    partnerTasksAdapter.notifyDataSetChanged()
                }
            }
    }
}
