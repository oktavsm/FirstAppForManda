package com.example.firstappformanda // <- SESUAIKAN

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

// Class ini harus mewarisi FirebaseMessagingService
class MyFirebaseMessagingService : FirebaseMessagingService() {

    /**
     * Fungsi ini akan dipanggil saat ada notifikasi masuk
     * KETIKA APLIKASI SEDANG DIBUKA (foreground).
     */
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        // Kita bisa lihat detail notifikasi yang masuk di Logcat
        Log.d("FCM", "From: ${remoteMessage.from}")

        // Cek apakah notifikasi punya data payload
        remoteMessage.data.isNotEmpty().let {
            Log.d("FCM", "Message data payload: " + remoteMessage.data)
        }

        // Cek apakah notifikasi punya body (pesan utama)
        remoteMessage.notification?.let {
            Log.d("FCM", "Message Notification Body: ${it.body}")
            // Di sini kita bisa buat notifikasi custom kita sendiri nanti
        }
    }

    /**
     * Fungsi ini akan dipanggil setiap kali ada device token baru
     * yang dibuat untuk HP ini.
     */
    // Di dalam MyFirebaseMessagingService.kt
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FCM", "Refreshed token: $token")
        sendRegistrationToServer(token)
    }

    private fun sendRegistrationToServer(token: String?) {
        val firebaseUser = FirebaseAuth.getInstance().currentUser
        if (firebaseUser != null && token != null) {
            val userId = firebaseUser.uid
            val firestore = FirebaseFirestore.getInstance()

            val tokenData = hashMapOf("fcmToken" to token)
            firestore.collection("users").document(userId)
                .set(tokenData, SetOptions.merge()) // Pakai metode yang sama
                .addOnSuccessListener { Log.d("FCM", "Token updated via service for user: $userId") }
        }
    }
}