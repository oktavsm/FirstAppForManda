package com.example.firstappformanda // <- SESUAIKAN

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.client.util.DateTime
import com.google.api.services.calendar.CalendarScopes
import com.google.api.services.calendar.model.Event
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class GoogleCalendarActivity : AppCompatActivity() {

    private lateinit var rvGoogleEvents: RecyclerView
    private lateinit var eventAdapter: GoogleEventAdapter
    private val eventList = mutableListOf<GoogleCalendarEvent>()
    private lateinit var googleSignInClient: GoogleSignInClient

    // Launcher modern untuk menangani SEMUA hasil dari Google (Login & Izin)
    private val googleApiLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // Jika user berhasil login atau memberikan izin, kita selalu cek ulang dari awal
            Log.d("GoogleCalendar", "ActivityResult OK. Re-checking permissions.")
            checkPermissionsAndFetchEvents()
        } else {
            Log.e("GoogleCalendar", "ActivityResult NOT OK. Result code: ${result.resultCode}")
            Toast.makeText(this, "Proses dibatalkan atau izin ditolak.", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_google_calendar)

        Log.d("GoogleCalendar", "onCreate: Activity started.")
        // Inisialisasi UI
        rvGoogleEvents = findViewById(R.id.rv_google_events)
        eventAdapter = GoogleEventAdapter(eventList)
        rvGoogleEvents.layoutManager = LinearLayoutManager(this)
        rvGoogleEvents.adapter = eventAdapter

        // Konfigurasi dan mulai alur pengecekan
        configureGoogleSignIn()
        checkPermissionsAndFetchEvents()
    }

    private fun configureGoogleSignIn() {
        Log.d("GoogleCalendar", "configureGoogleSignIn: Configuring Google Sign-In options.")
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestIdToken(getString(R.string.default_web_client_id)) // Minta "KTP Digital"
            .requestScopes(Scope(CalendarScopes.CALENDAR_EVENTS)) // Minta izin baca/tulis Kalender
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)
    }

    private fun checkPermissionsAndFetchEvents() {
        Log.d("GoogleCalendar", "checkPermissionsAndFetchEvents: Checking account and permissions.")
        val account = GoogleSignIn.getLastSignedInAccount(this)
        val calendarScope = Scope(CalendarScopes.CALENDAR_EVENTS)

        if (account == null) {
            // Jika belum pernah login sama sekali, mulai alur login
            Log.d("GoogleCalendar", "No account found. Launching Sign-In.")
            googleApiLauncher.launch(googleSignInClient.signInIntent)
            Log.d("GoogleCalendar", "Sign-In intent launched.")
            return
        }

        Log.d("GoogleCalendar", "Account found: ${account.email}")

        if (GoogleSignIn.hasPermissions(account, calendarScope)) {
            // Jika sudah punya izin, langsung ambil data
            Log.d("GoogleCalendar", "Calendar permission already granted. Fetching events.")
            loadEventsFromApi()
        } else {
            // Jika sudah login tapi belum punya izin, minta izin
            Log.d("GoogleCalendar", "Calendar permission not granted. Launching permission request via signInIntent.")
            googleApiLauncher.launch(googleSignInClient.signInIntent)
            Log.d("GoogleCalendar", "Permission request intent launched.")
        }
    }

    private fun loadEventsFromApi() {
        Log.d("GoogleCalendar", "loadEventsFromApi: Attempting to load events.")

        val account = GoogleSignIn.getLastSignedInAccount(this)!!

        val credential = GoogleAccountCredential.usingOAuth2(this, listOf(CalendarScopes.CALENDAR_EVENTS))
            .setSelectedAccount(account.account)

        val calendarService = com.google.api.services.calendar.Calendar.Builder(
            NetHttpTransport(),
            GsonFactory.getDefaultInstance(),
            credential
        )
            .setApplicationName(getString(R.string.app_name))
            .build()

        Toast.makeText(this, "Mengambil jadwal dari Google...", Toast.LENGTH_SHORT).show()
        Log.d("GoogleCalendar", "Starting Coroutine to fetch events.")

        // Jalankan di background agar UI tidak freeze
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val now = DateTime(System.currentTimeMillis())
                val events = calendarService.events().list("primary")
                    .setMaxResults(20) // Ambil 20 event terdekat
                    .setTimeMin(now)   // Mulai dari sekarang
                    .setQ("[FAFM]")    // Filter hanya yang punya tag [FAFM]
                    .setOrderBy("startTime")
                    .setSingleEvents(true)
                    .execute()

                val items: List<Event> = events.items ?: emptyList()
                Log.i("GoogleCalendar", "Fetched ${items.size} events from Google API with query [FAFM].")

                val formattedEvents = items.mapNotNull { event ->
                    val title = event.summary?.replace("[FAFM]", "")?.trim() ?: return@mapNotNull null
                    val start = event.start?.dateTime ?: event.start?.date ?: return@mapNotNull null
                    val gEvent = GoogleCalendarEvent(
                        title = title,
                        startTime = formatDateTime(start),
                        endTime = event.end?.dateTime?.let { formatDateTime(it) }
                    )
                    Log.d("GoogleCalendar", "Parsed event: ${gEvent.title} - ${gEvent.startTime}")
                    gEvent
                }

                withContext(Dispatchers.Main) {
                    Log.d("GoogleCalendar", "Updating UI with ${formattedEvents.size} events.")
                    eventList.clear()
                    eventList.addAll(formattedEvents)
                    eventAdapter.notifyDataSetChanged()
                    if (formattedEvents.isEmpty()) {
                        Toast.makeText(this@GoogleCalendarActivity, "Tidak ada jadwal dengan tag [FAFM] ditemukan.", Toast.LENGTH_LONG).show()
                        Log.i("GoogleCalendar", "No events with tag [FAFM] found.")
                    } else {
                        Log.i("GoogleCalendar", "Successfully displayed ${formattedEvents.size} events.")
                    }
                }

            } catch (userRecoverableAuthIOException: com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException) {
                Log.e("GoogleCalendar", "UserRecoverableAuthIOException: Needs new authorization. Launching intent.", userRecoverableAuthIOException)
                googleApiLauncher.launch(userRecoverableAuthIOException.intent)
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@GoogleCalendarActivity, "Gagal mengambil data: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun formatDateTime(dateTime: DateTime): String {
        return try {
            // Gunakan toString() untuk mendapatkan representasi string RFC3339 yang aman
            val rfc3339String = dateTime.toStringRfc3339()
            Log.v("GoogleCalendar", "formatDateTime: Formatting DateTime value: $rfc3339String")
            val date = Date(dateTime.value)
            if (dateTime.isDateOnly) {
                SimpleDateFormat("dd MMMM yyyy", Locale.getDefault()).format(date)
            } else {
                SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(date)
            }
        } catch (e: Exception) {
            Log.e("GoogleCalendar", "Error formatting DateTime: ${dateTime.toStringRfc3339()}", e)
            dateTime.toStringRfc3339() // Kembalikan string RFC3339 jika parsing gagal
        }
    }
}