package com.example.firstappformanda // <- SESUAIKAN

import android.app.Activity
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.client.util.DateTime
import com.google.api.services.calendar.CalendarScopes
import com.google.api.services.calendar.model.Event
import com.google.api.services.calendar.model.EventDateTime
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class ActivityDetailActivity : AppCompatActivity() {

    // --- Deklarasi Variabel UI ---
    private lateinit var etTitle: EditText
    private lateinit var etNotes: EditText
    private lateinit var tvStartTime: TextView
    private lateinit var tvEndTime: TextView
    private lateinit var btnSave: MaterialButton
    private lateinit var btnAddToCalendar: MaterialButton
    private lateinit var btnDelete: MaterialButton // <-- BARU

    // --- Deklarasi Variabel Sistem & Data ---
    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private var activityType: String? = null
    private var currentActivityId: String? = null
    private var isDoneStatus: Boolean = false
    private var currentGoogleEventId: String? = null

    private var startCalendar = Calendar.getInstance()
    private var endCalendar = Calendar.getInstance()

    // (BARU) Launcher untuk menangani hasil dari layar izin Google
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // Jika user memberikan izin, coba lagi sinkronkan ke kalender
            Toast.makeText(this, "Izin diberikan. Mencoba sinkronisasi lagi...", Toast.LENGTH_SHORT).show()
            addOrUpdateEventInGoogleCalendar()
        } else {
            Toast.makeText(this, "Izin ke Google Calendar ditolak.", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detail)

        initializeFirebase()
        initializeViews()

        // Ambil data dari Intent
        activityType = intent.getStringExtra("ACTIVITY_TYPE")
        currentActivityId = intent.getStringExtra("ACTIVITY_ID")
        val isReadOnly = intent.getBooleanExtra("IS_READ_ONLY", false) // <-- Ambil status

        etTitle.hint = if (activityType == "event") "Nama Jadwal" else "Nama Tugas"

        if (currentActivityId != null) {
            loadActivityData()
        } else {
            // Sembunyikan tombol jika membuat baru
            btnAddToCalendar.visibility = View.GONE
            btnDelete.visibility = View.GONE

        }

        // --- LOGIKA BARU UNTUK MODE READ-ONLY ---
        if (isReadOnly) {
            etTitle.isEnabled = false
            etNotes.isEnabled = false
            tvStartTime.isClickable = false
            tvEndTime.isClickable = false
            btnSave.visibility = View.GONE
            btnAddToCalendar.visibility = View.GONE
            btnDelete.visibility = View.GONE // Sembunyikan tombol hapus jika read-only
        } else {
            setupListeners() // Hanya pasang listener jika bisa diedit
        }
    }

    private fun initializeViews() {
        etTitle = findViewById(R.id.et_detail_title)
        etNotes = findViewById(R.id.et_detail_notes)
        tvStartTime = findViewById(R.id.tv_start_time)
        tvEndTime = findViewById(R.id.tv_end_time)
        btnSave = findViewById(R.id.btn_save_activity)
        btnAddToCalendar = findViewById(R.id.btn_add_to_calendar)
        btnDelete = findViewById(R.id.btn_delete_activity) // <-- Dihubungkan
    }

    // Hapus listener dari onCreate dan pindahkan ke fungsi sendiri
    private fun setupListeners() {
        tvStartTime.setOnClickListener { showDateTimePicker(startCalendar, tvStartTime) }
        tvEndTime.setOnClickListener { showDateTimePicker(endCalendar, tvEndTime) }
        btnSave.setOnClickListener { saveActivity() }
        btnAddToCalendar.setOnClickListener { addOrUpdateEventInGoogleCalendar() } // <-- Panggil fungsi baru
        btnDelete.setOnClickListener { showDeleteConfirmationDialog() } // <-- Listener baru
    }

    // --- FUNGSI BARU UNTUK HAPUS ---

    private fun showDeleteConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Hapus Aktivitas?")
            .setMessage("Yakin ingin menghapus '${etTitle.text}'? Aksi ini tidak bisa dibatalkan.")
            .setPositiveButton("Ya, Hapus") { _, _ ->
                deleteActivity()
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun deleteActivity() {
        if (currentActivityId == null) return

        // Jika ada ID Google Event, hapus dari Google Calendar DULU
        if (!currentGoogleEventId.isNullOrEmpty()) {
            deleteEventFromGoogleCalendar(currentGoogleEventId!!)
        } else {
            // Jika tidak ada, langsung hapus dari Firestore
            deleteActivityFromFirestore()
        }
    }

    private fun deleteEventFromGoogleCalendar(eventId: String) {
        val account = GoogleSignIn.getLastSignedInAccount(this) ?: return
        val credential = GoogleAccountCredential.usingOAuth2(this, listOf(CalendarScopes.CALENDAR_EVENTS)).setSelectedAccount(account.account)
        val calendarService = com.google.api.services.calendar.Calendar.Builder(
            NetHttpTransport(), GsonFactory.getDefaultInstance(), credential
        ).setApplicationName(getString(R.string.app_name)).build()

        Toast.makeText(this, "Menghapus dari Google Calendar...", Toast.LENGTH_SHORT).show()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                calendarService.events().delete("primary", eventId).execute()
                Log.d("GoogleCalendar", "Event $eventId deleted.")
                // Setelah berhasil di Google, baru hapus di Firestore
                deleteActivityFromFirestore()
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@ActivityDetailActivity, "Gagal hapus dari Google Calendar.", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun addOrUpdateEventInGoogleCalendar() {
        val account = GoogleSignIn.getLastSignedInAccount(this)
        if (account == null) {
            Toast.makeText(this, "Silakan login dengan Google terlebih dahulu.", Toast.LENGTH_SHORT).show()
            return
        }

        // --- VALIDASI WAKTU ---
        if (startCalendar.timeInMillis >= endCalendar.timeInMillis) {
            Toast.makeText(this, "Waktu selesai harus setelah waktu mulai.", Toast.LENGTH_LONG).show()
            return
        }

        val credential = GoogleAccountCredential.usingOAuth2(this, listOf(CalendarScopes.CALENDAR_EVENTS))
            .setSelectedAccount(account.account)

        val calendarService = com.google.api.services.calendar.Calendar.Builder(
            NetHttpTransport(), GsonFactory.getDefaultInstance(), credential
        ).setApplicationName(getString(R.string.app_name)).build()

        val title = etTitle.text.toString().trim()
        val notes = etNotes.text.toString().trim()
        val tag = "[FAFM]"
        val finalTitle = if (activityType == "task") "Tugas: $title $tag" else "$title $tag"

        // Siapkan detail event
        val event = com.google.api.services.calendar.model.Event()
            .setSummary(finalTitle)
            .setDescription(notes)
        val startTime = EventDateTime().setDateTime(DateTime(startCalendar.time))
        val endTime = EventDateTime().setDateTime(DateTime(endCalendar.time))
        event.start = startTime
        event.end = endTime

        val toastMessage = if (currentGoogleEventId.isNullOrEmpty()) "Menambahkan ke Google Calendar..."
                           else "Memperbarui di Google Calendar..."
        Toast.makeText(this, toastMessage, Toast.LENGTH_SHORT).show()


        // Jalankan di background
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val createdEvent: Event
                if (currentGoogleEventId.isNullOrEmpty()) {

                    // Jika belum ada ID -> Buat event baru
                    createdEvent = calendarService.events().insert("primary", event).execute()
                    Log.d("GoogleCalendar", "Event created: ${createdEvent.htmlLink}")

                } else {

                    // Jika sudah ada ID -> Update event yang ada
                    createdEvent = calendarService.events().update("primary", currentGoogleEventId, event).execute()
                    Log.d("GoogleCalendar", "Event updated: ${createdEvent.htmlLink}")

                }

                // Setelah berhasil, simpan ID Google Event ke Firestore
                saveGoogleEventIdToFirestore(createdEvent.id)

            } catch (e: UserRecoverableAuthIOException) {
                // --- INI BAGIAN PERBAIKANNYA ---
                // Jika kita "ditegur" karena butuh izin, kita sodorkan "buku tamu"
                withContext(Dispatchers.Main) {
                    requestPermissionLauncher.launch(e.intent)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@ActivityDetailActivity, "Gagal sinkronisasi: ${e.message}", Toast.LENGTH_LONG).show()

                }
            }
        }
    }

    private fun updateCalendarButtonState() {
        val isDateSet = tvStartTime.text != "Pilih tanggal & waktu" && tvEndTime.text != "Pilih tanggal & waktu"
        btnAddToCalendar.isEnabled = isDateSet
    }

    private fun showDateTimePicker(calendar: Calendar, textView: TextView) {
        DatePickerDialog(this, { _, year, month, day ->
            calendar.set(year, month, day)
            TimePickerDialog(this, { _, hour, minute ->
                calendar.set(Calendar.HOUR_OF_DAY, hour)
                calendar.set(Calendar.MINUTE, minute)
                val sdf = SimpleDateFormat("dd MMMM yyyy, HH:mm", Locale.getDefault())
                textView.text = sdf.format(calendar.time)
                updateCalendarButtonState()
            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show()
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun saveActivity() {
        val title = etTitle.text.toString().trim()
        val ownerId = auth.currentUser?.uid

        if (title.isEmpty()) {
            Toast.makeText(this, "Judul tidak boleh kosong", Toast.LENGTH_SHORT).show()
            return
        }
        if (tvStartTime.text.contains("Pilih") || tvEndTime.text.contains("Pilih")) {
            Toast.makeText(this, "Waktu mulai dan selesai harus diisi", Toast.LENGTH_SHORT).show()
            return
        }
        if (ownerId == null) {
            Toast.makeText(this, "Gagal mendapatkan info user.", Toast.LENGTH_SHORT).show()
            return
        }

        val activityMap = hashMapOf<String, Any?>(
            "type" to activityType,
            "title" to title,
            "notes" to etNotes.text.toString().trim(),
            "ownerId" to ownerId,
            "startTime" to startCalendar.timeInMillis,
            "endTime" to endCalendar.timeInMillis
        )

        if (activityType == "task") {
            activityMap["isDone"] = isDoneStatus
        }

        val activityRef = if (currentActivityId != null) {
            firestore.collection("activities").document(currentActivityId!!)
        } else {
            firestore.collection("activities").document()
        }

        activityRef.set(activityMap, SetOptions.merge())
            .addOnSuccessListener {
                Toast.makeText(this, "Aktivitas berhasil disimpan!", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Gagal menyimpan: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun loadActivityData() {
        firestore.collection("activities").document(currentActivityId!!).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    etTitle.setText(document.getString("title"))
                    etNotes.setText(document.getString("notes"))
                    isDoneStatus = document.getBoolean("isDone") ?: false

                    document.getLong("startTime")?.let {
                        startCalendar.timeInMillis = it
                        tvStartTime.text = formatTimestamp(it, "dd MMMM yyyy, HH:mm")
                    }
                    document.getLong("endTime")?.let {
                        endCalendar.timeInMillis = it
                        tvEndTime.text = formatTimestamp(it, "dd MMMM yyyy, HH:mm")
                    }
                    currentGoogleEventId = document.getString("googleEventId") // <-- Ambil ID Google

                    // Tampilkan tombol yang relevan jika mode edit
                    btnAddToCalendar.visibility = View.VISIBLE
                    btnDelete.visibility = View.VISIBLE

                    if (!currentGoogleEventId.isNullOrEmpty()) {
                        btnAddToCalendar.text = "Update di Google Calendar"
                    }
                    updateCalendarButtonState()
                }
            }
    }

    private fun deleteActivityFromFirestore() {
        firestore.collection("activities").document(currentActivityId!!)
            .delete()
            .addOnSuccessListener {
                runOnUiThread {
                    Toast.makeText(this, "Aktivitas berhasil dihapus.", Toast.LENGTH_SHORT).show()
                    finish() // Tutup halaman dan kembali ke daftar
                }
            }
            .addOnFailureListener { e ->
                runOnUiThread {
                    Toast.makeText(this, "Gagal hapus dari database: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun saveGoogleEventIdToFirestore(googleEventId: String) {
        if (currentActivityId == null) return
        firestore.collection("activities").document(currentActivityId!!)
            .update("googleEventId", googleEventId)
            .addOnSuccessListener {
                // Tampilkan pesan sukses di UI
                runOnUiThread {
                    Toast.makeText(this, "Berhasil disinkronkan!", Toast.LENGTH_SHORT).show()
                    this.currentGoogleEventId = googleEventId // Update ID di lokal
                }
            }
    }

    private fun formatTimestamp(timestamp: Long, format: String): String {
        return try {
            SimpleDateFormat(format, Locale.getDefault()).format(Date(timestamp))
        } catch (e: Exception) { "Invalid Date" }
    }

    private fun initializeFirebase() {
        firestore = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()
    }
}
