// Jangan lupa sesuaikan nama paketmu
package com.example.firstappformanda

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class TodoActivity : AppCompatActivity() {

    private lateinit var etTaskTitle: EditText
    private lateinit var btnAddTask: Button
    private lateinit var rvTasks: RecyclerView

    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    // Kita butuh Adapter untuk RecyclerView
    private lateinit var taskAdapter: TaskAdapter
    private val taskList = mutableListOf<Task>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_todo)

        // Inisialisasi Firebase
        firestore = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        // Hubungkan UI
        etTaskTitle = findViewById(R.id.et_task_title)
        btnAddTask = findViewById(R.id.btn_add_task)
        rvTasks = findViewById(R.id.rv_tasks)

        // Setup RecyclerView
        taskAdapter = TaskAdapter(taskList)
        rvTasks.layoutManager = LinearLayoutManager(this)
        rvTasks.adapter = taskAdapter

        // Muat data tugas saat activity dibuka
        loadTasks()

        // Event untuk tombol tambah tugas
        btnAddTask.setOnClickListener {
            val title = etTaskTitle.text.toString().trim()
            if (title.isNotEmpty()) {
                addTaskToFirestore(title)
            }
        }
    }

    private fun addTaskToFirestore(title: String) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(this, "Silakan login terlebih dahulu", Toast.LENGTH_SHORT).show()
            return
        }

        val task = hashMapOf(
            "title" to title,
            "isDone" to false,
            "ownerId" to currentUser.uid,
            "createdAt" to System.currentTimeMillis() // Opsional: untuk sorting
        )

        firestore.collection("tasks")
            .add(task) // .add() akan membuat ID dokumen acak
            .addOnSuccessListener {
                Toast.makeText(this, "Tugas ditambahkan!", Toast.LENGTH_SHORT).show()
                etTaskTitle.text.clear() // Kosongkan input
                loadTasks() // Muat ulang daftar tugas
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun loadTasks() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            // Handle jika user tidak login
            return
        }

        firestore.collection("tasks")
            .whereEqualTo("ownerId", currentUser.uid) // HANYA ambil tugas milik user ini
            .orderBy("createdAt", Query.Direction.DESCENDING) // Tampilkan yang terbaru di atas
            .get()
            .addOnSuccessListener { documents ->
                taskList.clear() // Kosongkan list lama
                for (document in documents) {
                    val task = document.toObject(Task::class.java).copy(id = document.id)
                    taskList.add(task)
                }
                taskAdapter.notifyDataSetChanged() // Beritahu adapter ada data baru
            }
            .addOnFailureListener { exception ->
                Log.w("TodoActivity", "Error getting documents: ", exception)
            }
    }
}