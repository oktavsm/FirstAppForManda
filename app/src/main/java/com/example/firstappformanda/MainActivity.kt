package com.example.firstappformanda // <- PASTIKAN INI SESUAI NAMA PAKETMU

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser

class MainActivity : AppCompatActivity() {

    // Deklarasikan Firebase Auth
    private lateinit var firebaseAuth: FirebaseAuth

    // Deklarasikan semua komponen UI
    private lateinit var layoutLoggedIn: LinearLayout
    private lateinit var layoutLoggedOut: LinearLayout
    private lateinit var tvWelcome: TextView
    private lateinit var btnLogout: Button
    private lateinit var btnToLogin: Button
    private lateinit var btnToRegister: Button


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Inisialisasi Firebase Auth
        firebaseAuth = FirebaseAuth.getInstance()

        // Hubungkan variabel dengan komponen di layout XML
        layoutLoggedIn = findViewById(R.id.layout_logged_in)
        layoutLoggedOut = findViewById(R.id.layout_logged_out)
        tvWelcome = findViewById(R.id.tv_welcome_message)
        btnLogout = findViewById(R.id.btn_logout)
        btnToLogin = findViewById(R.id.btn_to_login)
        btnToRegister = findViewById(R.id.btn_to_register)


        // Atur event klik untuk tombol-tombol
        btnToLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
        }

        btnToRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        btnLogout.setOnClickListener {
            firebaseAuth.signOut() // Proses logout dari Firebase
            updateUI(null)   // Update tampilan setelah logout
        }
    }

    override fun onStart() {
        super.onStart()
        // Cek status login setiap kali activity ini ditampilkan
        val currentUser = firebaseAuth.currentUser
        updateUI(currentUser)
    }

    // Fungsi khusus untuk mengatur tampilan berdasarkan status login
    private fun updateUI(currentUser: FirebaseUser?) {
        if (currentUser != null) {
            // Jika user sudah login
            layoutLoggedIn.visibility = View.VISIBLE
            layoutLoggedOut.visibility = View.GONE
            tvWelcome.text = "Selamat Datang,\n${currentUser.email}"
        } else {
            // Jika user belum login (atau sudah logout)
            layoutLoggedIn.visibility = View.GONE
            layoutLoggedOut.visibility = View.VISIBLE
        }
    }
}