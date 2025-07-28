package com.example.firstappformanda // <- SESUAIKAN

import android.app.Activity
import android.content.Intent
import android.app.DatePickerDialog
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class CoupleActivity : AppCompatActivity() {

    private lateinit var etPartnerEmail: EditText
    private lateinit var tvAnniversaryDate: TextView
    private lateinit var btnConnect: MaterialButton
    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    private var anniversaryCalendar = Calendar.getInstance()
    private var anniversaryDateString: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_couple)

        firestore = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()
        etPartnerEmail = findViewById(R.id.et_partner_email)
        tvAnniversaryDate = findViewById(R.id.tv_anniversary_date)
        btnConnect = findViewById(R.id.btn_connect)

        tvAnniversaryDate.setOnClickListener { showDatePicker() }

        btnConnect.setOnClickListener {
            val partnerEmail = etPartnerEmail.text.toString().trim()

            if (partnerEmail.isNotEmpty() && anniversaryDateString != null) {
                connectWithPartner(partnerEmail, anniversaryDateString!!)
            } else {
                Toast.makeText(this, "Email dan tanggal jadian harus diisi", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showDatePicker() {
        val datePickerDialog = DatePickerDialog(
            this, { _, year, month, dayOfMonth ->
                anniversaryCalendar.set(year, month, dayOfMonth)
                // Simpan dalam format YYYY-MM-DD agar mudah diolah
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                anniversaryDateString = sdf.format(anniversaryCalendar.time)

                // Tampilkan dalam format yang cantik
                val displaySdf = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault())
                tvAnniversaryDate.text = displaySdf.format(anniversaryCalendar.time)
            },
            anniversaryCalendar.get(Calendar.YEAR),
            anniversaryCalendar.get(Calendar.MONTH),
            anniversaryCalendar.get(Calendar.DAY_OF_MONTH)
        )
        datePickerDialog.show()
    }

    private fun connectWithPartner(partnerEmail: String, anniversaryDate: String) {
        val myUserId = auth.currentUser?.uid ?: return

        firestore.collection("users").whereEqualTo("email", partnerEmail).get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    Toast.makeText(this, "User dengan email tersebut tidak ditemukan", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                val partnerDocument = documents.first()
                val partnerId = partnerDocument.id
                val partnerCoupleId = partnerDocument.getString("coupleId")

                // --- LOGIKA CERDAS PENGECEKAN ---
                if (!partnerCoupleId.isNullOrEmpty()) {
                    Toast.makeText(this, "Gagal, pasanganmu sudah terhubung dengan orang lain.", Toast.LENGTH_LONG).show()
                    return@addOnSuccessListener
                }

                // Jika lolos, update kedua dokumen menggunakan batch write
                val batch = firestore.batch()
                val myDocRef = firestore.collection("users").document(myUserId)
                batch.update(myDocRef, "coupleId", partnerId, "anniversaryDate", anniversaryDate)

                val partnerDocRef = firestore.collection("users").document(partnerId)
                batch.update(partnerDocRef, "coupleId", myUserId, "anniversaryDate", anniversaryDate)

                batch.commit().addOnSuccessListener {
                    Toast.makeText(this, "Berhasil terhubung!", Toast.LENGTH_SHORT).show()
                    val resultIntent = Intent()
                    resultIntent.putExtra("refresh", true) // Sinyal untuk refresh
                    // Anda bisa menambahkan data tambahan ke intent jika diperlukan
                    setResult(Activity.RESULT_OK, resultIntent)
                    finish()
                }.addOnFailureListener { e ->
                    Toast.makeText(this, "Gagal terhubung: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }
}
