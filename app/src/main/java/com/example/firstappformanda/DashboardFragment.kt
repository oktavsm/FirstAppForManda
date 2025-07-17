package com.example.firstappformanda // <- SESUAIKAN

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.Scope
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.client.util.DateTime
import com.google.api.services.calendar.CalendarScopes
import com.google.api.services.calendar.model.EventDateTime
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Calendar
import java.util.Date
import java.util.Locale

class DashboardFragment : Fragment() {

    // --- Deklarasi Variabel ---
    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    // UI Kartu Detail Hubungan
    private lateinit var tvCoupleNames: TextView
    private lateinit var tvAnniversaryInfo: TextView
    private lateinit var tvDaysTogether: TextView

    // UI Kartu Status Pasangan
    private lateinit var cardPartnerStatus: androidx.cardview.widget.CardView
    private lateinit var tvPartnerStatusTitle: TextView
    private lateinit var tvPartnerLastNote: TextView
    private lateinit var tvPartnerMood: TextView
    private lateinit var layoutPartnerPhase: LinearLayout
    private lateinit var tvPartnerPhase: TextView

    // UI Agenda Pribadi
    private lateinit var rvMyAgenda: RecyclerView
    private lateinit var myAgendaAdapter: AgendaAdapter
    private val myAgendaList = mutableListOf<AgendaItem>()
    private lateinit var pbMyAgenda: ProgressBar
    private lateinit var tvEmptyMyAgenda: TextView

    // UI Agenda Pasangan
    private lateinit var cardPartnerAgenda: androidx.cardview.widget.CardView
    private lateinit var tvPartnerAgendaTitle: TextView
    private lateinit var rvPartnerAgenda: RecyclerView
    private lateinit var partnerAgendaAdapter: AgendaAdapter
    private val partnerAgendaList = mutableListOf<AgendaItem>()
    private lateinit var pbPartnerAgenda: ProgressBar
    private lateinit var tvEmptyPartnerAgenda: TextView

    private val TAG = "DashboardFragment"
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.getDefault())

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_dashboard, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initializeFirebase()
        initializeViews(view)
        setupAdapters()
    }

    override fun onResume() {
        super.onResume()
        // Muat ulang data setiap kali fragment ini kembali terlihat
        loadAllDashboardData()
    }

    private fun initializeFirebase() {
        firestore = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()
    }

    private fun initializeViews(view: View) {
        tvCoupleNames = view.findViewById(R.id.tv_couple_names)
        tvAnniversaryInfo = view.findViewById(R.id.tv_anniversary_info)
        tvDaysTogether = view.findViewById(R.id.tv_days_together)
        cardPartnerStatus = view.findViewById(R.id.card_partner_status)
        tvPartnerStatusTitle = view.findViewById(R.id.tv_partner_status_title)
        tvPartnerLastNote = view.findViewById(R.id.tv_partner_last_note)
        tvPartnerMood = view.findViewById(R.id.tv_partner_mood)
        layoutPartnerPhase = view.findViewById(R.id.layout_partner_phase)
        tvPartnerPhase = view.findViewById(R.id.tv_partner_phase)
        rvMyAgenda = view.findViewById(R.id.rv_my_agenda)
        pbMyAgenda = view.findViewById(R.id.progress_bar_my_agenda)
        tvEmptyMyAgenda = view.findViewById(R.id.tv_empty_my_agenda)
        cardPartnerAgenda = view.findViewById(R.id.card_partner_agenda)
        tvPartnerAgendaTitle = view.findViewById(R.id.tv_partner_agenda_title)
        rvPartnerAgenda = view.findViewById(R.id.rv_partner_agenda)
        pbPartnerAgenda = view.findViewById(R.id.progress_bar_partner_agenda)
        tvEmptyPartnerAgenda = view.findViewById(R.id.tv_empty_partner_agenda)
    }

    private fun setupAdapters() {
        myAgendaAdapter = AgendaAdapter(myAgendaList)
        rvMyAgenda.layoutManager = LinearLayoutManager(requireContext())
        rvMyAgenda.adapter = myAgendaAdapter

        partnerAgendaAdapter = AgendaAdapter(partnerAgendaList)
        rvPartnerAgenda.layoutManager = LinearLayoutManager(requireContext())
        rvPartnerAgenda.adapter = partnerAgendaAdapter
    }

    private fun loadAllDashboardData() {
        val myUserId = auth.currentUser?.uid ?: return

        loadUserAgenda(myUserId, myAgendaList, myAgendaAdapter, pbMyAgenda, tvEmptyMyAgenda, true)

        firestore.collection("users").document(myUserId).get()
            .addOnSuccessListener { myDocument ->
                val myName = myDocument.getString("nama") ?: "Kamu"
                val coupleId = myDocument.getString("coupleId")

                if (!coupleId.isNullOrEmpty()) {
                    firestore.collection("users").document(coupleId).get()
                        .addOnSuccessListener { partnerDocument ->
                            val partnerName = partnerDocument.getString("nama") ?: "Pasangan"
                            val partnerGender = partnerDocument.getString("gender")

                            cardPartnerStatus.visibility = View.VISIBLE
                            cardPartnerAgenda.visibility = View.VISIBLE

                            tvCoupleNames.text = "$myName & $partnerName"
                            val anniversaryDateStr = myDocument.getString("anniversaryDate")
                            if (!anniversaryDateStr.isNullOrEmpty()) {
                                try {
                                    val anniversaryDate = LocalDate.parse(anniversaryDateStr, dateFormatter)
                                    val daysTogether = ChronoUnit.DAYS.between(anniversaryDate, LocalDate.now())
                                    tvAnniversaryInfo.text = "Bersama sejak ${anniversaryDate.format(DateTimeFormatter.ofPattern("dd MMMM yyyy"))}"
                                    tvDaysTogether.text = "$daysTogether hari"
                                } catch (e: Exception) {
                                    Log.e(TAG, "Error parsing anniversary date", e)
                                }
                            }

                            tvPartnerStatusTitle.text = "Update dari $partnerName"
                            tvPartnerAgendaTitle.text = "Agenda $partnerName Hari Ini"
                            loadPartnerStatus(coupleId, partnerGender)
                            loadUserAgenda(coupleId, partnerAgendaList, partnerAgendaAdapter, pbPartnerAgenda, tvEmptyPartnerAgenda, false)
                        }
                } else {
                    tvCoupleNames.text = myName
                    tvAnniversaryInfo.text = "Hubungkan akun dengan pasanganmu"
                    tvDaysTogether.text = ""
                    cardPartnerStatus.visibility = View.GONE
                    cardPartnerAgenda.visibility = View.GONE
                }
            }
    }

    private fun loadUserAgenda(userId: String, list: MutableList<AgendaItem>, adapter: AgendaAdapter, progressBar: ProgressBar, emptyView: TextView, fetchFromGoogle: Boolean) {
        progressBar.visibility = View.VISIBLE
        emptyView.visibility = View.GONE

        val googleEvents = mutableListOf<AgendaItem>()
        val firestoreActivities = mutableListOf<AgendaItem>()
        var googleDone = !fetchFromGoogle
        var firestoreDone = false

        fun mergeAndDisplay() {
            if (googleDone && firestoreDone) {
                list.clear()
                list.addAll(googleEvents)
                list.addAll(firestoreActivities)
                list.sortBy { it.timestamp }
                adapter.notifyDataSetChanged()
                progressBar.visibility = View.GONE
                emptyView.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
            }
        }

        fetchFirestoreUnsyncedActivitiesForToday(userId) { activities ->
            firestoreActivities.addAll(activities)
            firestoreDone = true
            mergeAndDisplay()
        }

        if (fetchFromGoogle) {
            fetchGoogleCalendarEventsForToday { events ->
                googleEvents.addAll(events)
                googleDone = true
                mergeAndDisplay()
            }
        }
    }

    private fun loadPartnerStatus(partnerId: String, partnerGender: String?) {
        firestore.collection("daily_logs").whereEqualTo("ownerId", partnerId)
            .orderBy("date", Query.Direction.DESCENDING).limit(1).get()
            .addOnSuccessListener {
                if (!it.isEmpty) {
                    val lastLog = it.documents[0]
                    tvPartnerMood.text = lastLog.getString("mood")?.split(" ")?.get(0) ?: "❓"
                    tvPartnerLastNote.text = "Catatan terakhir: \"${lastLog.getString("notes")}\""
                } else {
                    tvPartnerMood.text = "❓"
                    tvPartnerLastNote.text = "Belum ada catatan."
                }
            }

        if (partnerGender == "Wanita") {
            layoutPartnerPhase.visibility = View.VISIBLE
            firestore.collection("cycle_logs").whereEqualTo("ownerId", partnerId)
                .orderBy("startDate", Query.Direction.ASCENDING).get()
                .addOnSuccessListener {
                    if(it.size() >= 2) {
                        val dates = it.documents.map { doc -> LocalDate.parse(doc.getString("startDate"), dateFormatter) }
                        val cycleDurations = (0 until dates.size - 1).map { i -> ChronoUnit.DAYS.between(dates[i], dates[i + 1]) }
                        val averageDuration = cycleDurations.average().toLong()
                        calculateAndDisplayCurrentPhase(dates.last(), averageDuration)
                    } else {
                        tvPartnerPhase.text = "Data Kurang"
                    }
                }
        } else {
            layoutPartnerPhase.visibility = View.GONE
        }
    }

    private fun fetchFirestoreUnsyncedActivitiesForToday(userId: String, onComplete: (List<AgendaItem>) -> Unit) {
        val calendar = Calendar.getInstance()
        val startOfDay = calendar.apply { set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0) }.timeInMillis
        val endOfDay = calendar.apply { set(Calendar.HOUR_OF_DAY, 23); set(Calendar.MINUTE, 59); set(Calendar.SECOND, 59) }.timeInMillis

        firestore.collection("activities")
            .whereEqualTo("ownerId", userId)
            .whereGreaterThanOrEqualTo("startTime", startOfDay)
            .whereLessThanOrEqualTo("startTime", endOfDay)
            .get()
            .addOnSuccessListener { documents ->
                val activities = documents.filter {
                    it.getString("googleEventId").isNullOrEmpty()
                }.map { doc ->
                    val activity = doc.toObject(ActivityModel::class.java)
                    val timeInfo = "${formatTimestamp(activity.startTime, "HH:mm")} - ${formatTimestamp(activity.endTime, "HH:mm")}"
                    AgendaItem(
                        timestamp = activity.startTime ?: 0,
                        title = activity.title ?: "Tanpa Judul",
                        subtitle = if (activity.type == "task") "Tugas: $timeInfo" else "Jadwal: $timeInfo",
                        type = activity.type ?: "task"
                    )
                }
                onComplete(activities)
            }
            .addOnFailureListener { onComplete(emptyList()) }
    }

    private fun fetchGoogleCalendarEventsForToday(onComplete: (List<AgendaItem>) -> Unit) {
        val account = GoogleSignIn.getLastSignedInAccount(requireContext())
        if (account == null || !GoogleSignIn.hasPermissions(account, Scope(CalendarScopes.CALENDAR_EVENTS))) {
            onComplete(emptyList()); return
        }
        val credential = GoogleAccountCredential.usingOAuth2(requireContext(), listOf(CalendarScopes.CALENDAR_EVENTS)).setSelectedAccount(account.account)
        val calendarService = com.google.api.services.calendar.Calendar.Builder(
            NetHttpTransport(), GsonFactory.getDefaultInstance(), credential
        ).setApplicationName(getString(R.string.app_name)).build()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val calendar = Calendar.getInstance()
                val startOfDay = DateTime(calendar.apply { set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0) }.time)
                val endOfDay = DateTime(calendar.apply { set(Calendar.HOUR_OF_DAY, 23); set(Calendar.MINUTE, 59) }.time)

                val events = calendarService.events().list("primary")
                    .setTimeMin(startOfDay).setTimeMax(endOfDay)
                    .setOrderBy("startTime").setSingleEvents(true).execute()

                val items = events.items?.mapNotNull { event ->
                    val startTime = event.start?.dateTime?.value ?: event.start?.date?.value ?: return@mapNotNull null
                    AgendaItem(
                        timestamp = startTime,
                        title = event.summary?.replace("[FAFM]", "")?.trim() ?: "Tanpa Judul",
                        subtitle = "Google Calendar: ${formatEventTime(event.start, event.end)}",
                        type = "event"
                    )
                } ?: emptyList()
                withContext(Dispatchers.Main) { onComplete(items) }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { onComplete(emptyList()) }
            }
        }
    }

    private fun calculateAndDisplayCurrentPhase(lastStartDate: LocalDate, avgCycleLength: Long) {
        val today = LocalDate.now()
        val dayOfCycle = ChronoUnit.DAYS.between(lastStartDate, today) + 1
        tvPartnerPhase.text = when {
            dayOfCycle in 1..5 -> "Fase Menstruasi"
            dayOfCycle in 6..14 -> "Fase Folikular"
            dayOfCycle in 15..17 -> "Fase Ovulasi"
            dayOfCycle > 18 && dayOfCycle <= avgCycleLength -> "Fase Luteal (PMS)"
            else -> "Siklus Selesai"
        }
    }

    private fun formatEventTime(start: EventDateTime, end: EventDateTime): String {
        val startTime = start.dateTime?.value ?: start.date.value
        val endTime = end.dateTime?.value ?: end.date.value
        return if (start.dateTime == null) "Seharian" else "${formatTimestamp(startTime, "HH:mm")} - ${formatTimestamp(endTime, "HH:mm")}"
    }

    private fun formatTimestamp(timestamp: Long?, format: String = "HH:mm"): String {
        if (timestamp == null) return ""
        return SimpleDateFormat(format, Locale.getDefault()).format(Date(timestamp))
    }
}
