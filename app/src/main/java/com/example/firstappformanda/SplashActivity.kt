package com.example.firstappformanda // <- SESUAIKAN

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        // Gunakan Handler untuk memberi jeda sebelum pindah halaman
        Handler(Looper.getMainLooper()).postDelayed({

            // Cek apakah user sudah pernah menyelesaikan onboarding
            val prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE)
            val isOnboardingCompleted = prefs.getBoolean("onboarding_completed", false)

            if (!isOnboardingCompleted) {
                // Jika belum, arahkan ke Onboarding
                startActivity(Intent(this, OnboardingActivity::class.java))
            } else {
                // Jika sudah, cek status login
                if (FirebaseAuth.getInstance().currentUser != null) {
                    // Jika sudah login, langsung ke MainActivity
                    startActivity(Intent(this, MainActivity::class.java))
                } else {
                    // Jika belum login, ke halaman Auth (Login/Register)
                    startActivity(Intent(this, AuthActivity::class.java))
                }
            }
            finish() // Tutup SplashActivity agar tidak bisa kembali
        }, 2000) // Jeda 2 detik (2000 milidetik)
    }
}