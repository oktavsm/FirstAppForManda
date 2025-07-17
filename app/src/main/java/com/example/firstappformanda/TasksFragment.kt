package com.example.firstappformanda // <- SESUAIKAN

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class TasksFragment : Fragment() {

    private lateinit var rvTasks: RecyclerView
    private lateinit var fabAddTask: FloatingActionButton
    private lateinit var activityAdapter: ActivityAdapter
    private val activityList = mutableListOf<ActivityModel>()

    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_tasks, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        firestore = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        rvTasks = view.findViewById(R.id.rv_tasks)
        fabAddTask = view.findViewById(R.id.fab_add_task)

        activityAdapter = ActivityAdapter(activityList)
        rvTasks.layoutManager = LinearLayoutManager(requireContext())
        rvTasks.adapter = activityAdapter

        loadTasks()

        fabAddTask.setOnClickListener {
            val intent = Intent(requireContext(), ActivityDetailActivity::class.java)
            intent.putExtra("ACTIVITY_TYPE", "task")
            startActivity(intent)
        }
    }

    private fun loadTasks() {
        val userId = auth.currentUser?.uid ?: return

        firestore.collection("activities")
            .whereEqualTo("ownerId", userId)
            .whereEqualTo("type", "task")
            .orderBy("endTime", Query.Direction.ASCENDING) // <-- DIUBAH dari deadline ke endTime
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Log.w("TasksFragment", "Listen failed.", e)
                    return@addSnapshotListener
                }
                if (snapshots != null) {
                    activityList.clear()
                    for (document in snapshots) {
                        val activity = document.toObject(ActivityModel::class.java).copy(id = document.id)
                        activityList.add(activity)
                    }
                    activityAdapter.notifyDataSetChanged()
                }
            }
    }
}
