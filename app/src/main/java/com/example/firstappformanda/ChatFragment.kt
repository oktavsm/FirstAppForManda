package com.example.firstappformanda // <- SESUAIKAN

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class ChatFragment : Fragment() {

    // --- Deklarasi Variabel ---
    private lateinit var rvChat: RecyclerView
    private lateinit var etMessage: EditText
    private lateinit var btnSend: MaterialButton
    private lateinit var chatAdapter: ChatAdapter
    private val messageList = mutableListOf<Message>()

    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    private var chatRoomId: String? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_chat, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Inisialisasi
        firestore = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()
        initializeViews(view)

        // Setup RecyclerView & Adapter
        chatAdapter = ChatAdapter(messageList)
        val layoutManager = LinearLayoutManager(requireContext())
        rvChat.layoutManager = layoutManager
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

    private fun initializeViews(view: View) {
        rvChat = view.findViewById(R.id.rv_chat_messages)
        etMessage = view.findViewById(R.id.et_chat_message)
        btnSend = view.findViewById(R.id.btn_send_chat)
    }

    // --- Sisa semua fungsi di bawah ini SAMA PERSIS seperti di ChatActivity ---
    // --- Tidak ada yang perlu diubah, cukup copy-paste ---

    private fun setupChatRoom() {
        val myUserId = auth.currentUser?.uid ?: return
        firestore.collection("users").document(myUserId).get()
            .addOnSuccessListener { document ->
                val partnerId = document.getString("coupleId")
                if (partnerId.isNullOrEmpty()) {
                    // Handle jika belum terhubung, misal tampilkan pesan
                    btnSend.isEnabled = false
                    etMessage.hint = "Hubungkan akun dengan pasangan dulu..."
                    return@addOnSuccessListener
                }

                chatRoomId = if (myUserId < partnerId) "${myUserId}_${partnerId}" else "${partnerId}_${myUserId}"
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

        firestore.collection("chats").document(chatRoomId!!)
            .collection("messages").add(message)
            .addOnSuccessListener {
                etMessage.text.clear()
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Gagal mengirim pesan: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun listenForMessages() {
        if (chatRoomId == null) return

        firestore.collection("chats").document(chatRoomId!!)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshots, e ->
                if (e != null) { return@addSnapshotListener }
                if (snapshots != null) {
                    messageList.clear()
                    for (doc in snapshots.documents) {
                        val message = doc.toObject(Message::class.java)
                        if (message != null) {
                            messageList.add(message)
                        }
                    }
                    chatAdapter.notifyDataSetChanged()
                    rvChat.scrollToPosition(messageList.size - 1)
                }
            }
    }
}
