package com.example.firstappformanda // <- PASTIKAN INI SESUAI NAMA PAKETMU

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore

class RegisterActivity : AppCompatActivity() {

    // Deklarasikan variabel untuk Firebase Auth dan komponen UI
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnRegister: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register) // Menghubungkan ke file XML

        // Inisialisasi Firebase Auth
        firebaseAuth = FirebaseAuth.getInstance()

        // Menghubungkan variabel dengan komponen di layout XML berdasarkan ID
        etEmail = findViewById(R.id.et_register_email)
        etPassword = findViewById(R.id.et_register_password)
        btnRegister = findViewById(R.id.btn_register)

        // Memberi 'event' ketika tombol register di-klik
        btnRegister.setOnClickListener {
            // Ambil teks dari EditText
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            // Validasi sederhana: pastikan tidak kosong
            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Email dan Password tidak boleh kosong!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Memanggil fungsi Firebase untuk membuat user baru
            firebaseAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        // Jika berhasil
                        Toast.makeText(this, "Registrasi berhasil!", Toast.LENGTH_SHORT).show()
// Di dalam RegisterActivity.kt -> onCreate -> btnRegister.setOnClickListener -> if (task.isSuccessful)

// ... setelah Toast.makeText(this, "Registrasi berhasil!", Toast.LENGTH_SHORT).show()

// 1. Ambil ID unik dari user yang baru dibuat
                        val firebaseUser: FirebaseUser = task.result.user!!
                        val userId: String = firebaseUser.uid

// 2. Siapkan data dalam bentuk Map
                        val userMap = hashMapOf(
                            "userId" to userId,
                            "email" to email, // 'email' diambil dari variabel yang sudah ada di atasnya
                            "nama" to "",     // Untuk sementara namanya kosong
                            "coupleId" to ""  // Couple ID juga kosong
                        )

// 3. Dapatkan akses ke Firestore dan simpan datanya
                        val firestore = FirebaseFirestore.getInstance()
                        firestore.collection("users").document(userId) // Buat collection "users" & document dengan ID user
                            .set(userMap) // Simpan data Map ke dokumen itu
                            .addOnSuccessListener {
                                // Ini akan berjalan jika data BERHASIL disimpan ke Firestore
                                Toast.makeText(this, "Data profil berhasil dibuat!", Toast.LENGTH_SHORT).show()
                            }
                            .addOnFailureListener { e ->
                                // Ini akan berjalan jika data GAGAL disimpan
                                Toast.makeText(this, "Error saat membuat profil: ${e.message}", Toast.LENGTH_LONG).show()
                            }

// Kode untuk pindah ke LoginActivity...
                        val intent = Intent(this, LoginActivity::class.java)
                        startActivity(intent)
                        finish()
                    } else {
                        // Jika gagal, tampilkan pesan error
                        Toast.makeText(this, "Registrasi Gagal: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                    }
                }
        }
    }
}