package com.example.firstappformanda // <- SESUAIKAN

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class ChatActivity : AppCompatActivity() {

    private lateinit var rvChat: RecyclerView
    private lateinit var etMessage: EditText
    private lateinit var btnSend: Button
    private lateinit var chatAdapter: ChatAdapter
    private val messageList = mutableListOf<Message>()

    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    private var chatRoomId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        // Inisialisasi Firebase
        firestore = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        // Hubungkan UI
        rvChat = findViewById(R.id.rv_chat_messages)
        etMessage = findViewById(R.id.et_chat_message)
        btnSend = findViewById(R.id.btn_send_chat)

        // Setup RecyclerView & Adapter
        chatAdapter = ChatAdapter(messageList)
        rvChat.layoutManager = LinearLayoutManager(this)
        rvChat.adapter = chatAdapter

        // Dapatkan ID pasangan dan siapkan ruang obrolan
        setupChatRoom()

        // Atur listener untuk tombol kirim
        btnSend.setOnClickListener {
            val messageText = etMessage.text.toString().trim()
            if (messageText.isNotEmpty()) {
                sendMessage(messageText)
            }
        }
    }

    private fun setupChatRoom() {
        val myUserId = auth.currentUser?.uid ?: return

        // Ambil ID pasangan dari profil kita
        firestore.collection("users").document(myUserId).get()
            .addOnSuccessListener { document ->
                val partnerId = document.getString("coupleId")
                if (partnerId.isNullOrEmpty()) {
                    Toast.makeText(this, "Kamu belum terhubung dengan pasangan", Toast.LENGTH_SHORT).show()
                    finish() // Tutup halaman chat jika tidak punya pasangan
                    return@addOnSuccessListener
                }

                // Buat ID ruang chat yang konsisten
                chatRoomId = if (myUserId < partnerId) {
                    "${myUserId}_${partnerId}"
                } else {
                    "${partnerId}_${myUserId}"
                }

                // Mulai mendengarkan pesan baru
                listenForMessages()
            }
    }

    private fun sendMessage(messageText: String) {
        val myUserId = auth.currentUser?.uid ?: return
        if (chatRoomId == null) return

        val message = Message(
            senderId = myUserId,
            text = messageText,
            timestamp = System.currentTimeMillis()
        )

        // Simpan pesan ke sub-collection 'messages' di dalam ruang chat
        firestore.collection("chats").document(chatRoomId!!)
            .collection("messages").add(message)
            .addOnSuccessListener {
                // Kosongkan input setelah berhasil terkirim
                etMessage.text.clear()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Gagal mengirim pesan: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun listenForMessages() {
        if (chatRoomId == null) return

        // Dengarkan perubahan di sub-collection 'messages', urutkan berdasarkan waktu
        firestore.collection("chats").document(chatRoomId!!)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    // Handle error
                    return@addSnapshotListener
                }

                if (snapshots != null) {
                    messageList.clear()
                    for (doc in snapshots.documents) {
                        val message = doc.toObject(Message::class.java)
                        if (message != null) {
                            messageList.add(message)
                        }
                    }
                    chatAdapter.notifyDataSetChanged()
                    // Otomatis scroll ke pesan paling bawah
                    rvChat.scrollToPosition(messageList.size - 1)
                }
            }
    }
}