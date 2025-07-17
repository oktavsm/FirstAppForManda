package com.example.firstappformanda // <- SESUAIKAN

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.RadioGroup
import android.widget.Toast
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class GenderSelectionActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gender_selection)

        val rgGender: RadioGroup = findViewById(R.id.rg_gender_selection)
        val btnSave: MaterialButton = findViewById(R.id.btn_save_gender)

        btnSave.setOnClickListener {
            val selectedGenderId = rgGender.checkedRadioButtonId
            if (selectedGenderId == -1) {
                Toast.makeText(this, "Silakan pilih gender", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val gender = if (selectedGenderId == R.id.rb_male_selection) "Pria" else "Wanita"
            updateGenderInFirestore(gender)
        }
    }

    private fun updateGenderInFirestore(gender: String) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId == null) {
            // Jika terjadi error aneh, lempar ke halaman login
            startActivity(Intent(this, AuthActivity::class.java))
            finish()
            return
        }

        FirebaseFirestore.getInstance().collection("users").document(userId)
            .update("gender", gender)
            .addOnSuccessListener {
                Toast.makeText(this, "Profil berhasil diperbarui!", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, MainActivity::class.java))
                finishAffinity() // Tutup semua activity sebelumnya
            }
            .addOnFailureListener {
                Toast.makeText(this, "Gagal menyimpan gender.", Toast.LENGTH_SHORT).show()
            }
    }
}