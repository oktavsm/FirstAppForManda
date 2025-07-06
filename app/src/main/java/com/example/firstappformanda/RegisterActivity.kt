package com.example.firstappformanda // <- PASTIKAN INI SESUAI NAMA PAKETMU

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth

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
                        // Pindah ke halaman Login setelah berhasil daftar
                        val intent = Intent(this, LoginActivity::class.java)
                        startActivity(intent)
                        finish() // Tutup activity ini agar tidak bisa kembali
                    } else {
                        // Jika gagal, tampilkan pesan error
                        Toast.makeText(this, "Registrasi Gagal: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                    }
                }
        }
    }
}