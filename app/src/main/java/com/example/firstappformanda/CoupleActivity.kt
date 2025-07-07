package com.example.firstappformanda // <- PASTIKAN SESUAI NAMA PAKETMU

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class CoupleActivity : AppCompatActivity() {

    private lateinit var etPartnerEmail: EditText
    private lateinit var btnConnect: Button

    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_couple)

        // Inisialisasi
        firestore = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        // Hubungkan ke UI
        etPartnerEmail = findViewById(R.id.et_partner_email)
        btnConnect = findViewById(R.id.btn_connect)

        btnConnect.setOnClickListener {
            val partnerEmail = etPartnerEmail.text.toString().trim()
            if (partnerEmail.isNotEmpty()) {
                connectWithPartner(partnerEmail)
            } else {
                Toast.makeText(this, "Email tidak boleh kosong", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun connectWithPartner(partnerEmail: String) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(this, "Kamu harus login dulu", Toast.LENGTH_SHORT).show()
            return
        }
        val myUserId = currentUser.uid

        // Langkah 1: Cari user pasangan berdasarkan email
        firestore.collection("users")
            .whereEqualTo("email", partnerEmail)
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    // Jika tidak ada user dengan email itu
                    Toast.makeText(this, "User dengan email tersebut tidak ditemukan", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                // Jika user ditemukan, ambil ID-nya
                val partnerDocument = documents.first()
                val partnerId = partnerDocument.id

                // Langkah 2: Update dokumen kita & dokumen pasangan
                // Update dokumen kita
                firestore.collection("users").document(myUserId)
                    .update("coupleId", partnerId)
                    .addOnSuccessListener {
                        // Update dokumen pasangan
                        firestore.collection("users").document(partnerId)
                            .update("coupleId", myUserId)
                            .addOnSuccessListener {
                                Toast.makeText(this, "Berhasil terhubung!", Toast.LENGTH_SHORT).show()
                                finish() // Tutup halaman ini setelah berhasil
                            }
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Gagal terhubung: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error mencari user: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}