package com.example.firstappformanda // <- SESUAIKAN

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.kizitonwose.calendar.core.CalendarDay
import com.kizitonwose.calendar.core.DayPosition
import com.kizitonwose.calendar.core.daysOfWeek
import com.kizitonwose.calendar.view.CalendarView
import com.kizitonwose.calendar.view.MonthDayBinder
import com.kizitonwose.calendar.view.ViewContainer
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

class PartnerMonitoringFragment : Fragment() {

    // --- Deklarasi Variabel ---
    private lateinit var calendarView: CalendarView
    private lateinit var tvMonthYear: TextView
    private lateinit var layoutPrediction: LinearLayout
    private lateinit var tvPrediction: TextView
    private lateinit var tvPhase: TextView
    private lateinit var tvPhaseInfo: TextView
    private lateinit var etReminder: EditText
    private lateinit var btnSendReminder: MaterialButton
    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    private var partnerId: String? = null
    private val cycleStartDates = mutableSetOf<LocalDate>()
    private val datesWithLogs = mutableSetOf<LocalDate>()
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_partner_monitoring, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        firestore = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        initializeViews(view)
        loadPartnerData()
    }

    private fun initializeViews(view: View) {
        tvMonthYear = view.findViewById(R.id.tv_month_year)
        calendarView = view.findViewById(R.id.calendar_view)
        layoutPrediction = view.findViewById(R.id.layout_prediction_and_phase)
        tvPrediction = view.findViewById(R.id.tv_my_prediction)
        tvPhase = view.findViewById(R.id.tv_my_current_phase)
        tvPhaseInfo = view.findViewById(R.id.tv_my_phase_info)
        etReminder = view.findViewById(R.id.et_reminder_message)
        btnSendReminder = view.findViewById(R.id.btn_send_reminder)

        view.findViewById<ImageView>(R.id.iv_calendar_forward).setOnClickListener {
            calendarView.findFirstVisibleMonth()?.let { calendarView.smoothScrollToMonth(it.yearMonth.plusMonths(1)) }
        }
        view.findViewById<ImageView>(R.id.iv_calendar_back).setOnClickListener {
            calendarView.findFirstVisibleMonth()?.let { calendarView.smoothScrollToMonth(it.yearMonth.minusMonths(1)) }
        }

        btnSendReminder.setOnClickListener {
            val message = etReminder.text.toString().trim()
            if (message.isNotEmpty() && partnerId != null) {
                sendNotificationRequest(message)
            } else {
                Toast.makeText(requireContext(), "Pesan tidak boleh kosong atau pasangan tidak ditemukan.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadPartnerData() {
        val myUserId = auth.currentUser?.uid ?: return
        firestore.collection("users").document(myUserId).get()
            .addOnSuccessListener { myDocument ->
                val coupleId = myDocument.getString("coupleId")
                if (coupleId.isNullOrEmpty()) {
                    tvMonthYear.text = "Belum Terhubung"
                    calendarView.visibility = View.GONE
                    layoutPrediction.visibility = View.GONE
                    view?.findViewById<LinearLayout>(R.id.layout_send_reminder)?.visibility = View.GONE
                    return@addOnSuccessListener
                }
                // this.partnerId sudah di-set saat mengambil data coupleId dari Firestore.
                checkPartnerGenderAndLoadData(coupleId)
            }
    }

    private fun checkPartnerGenderAndLoadData(partnerId: String) {
        firestore.collection("users").document(partnerId).get()
            .addOnSuccessListener { partnerDocument ->
                val partnerGender = partnerDocument.getString("gender")
                this.partnerId = partnerId // Simpan partnerId di sini setelah memastikan partner ada
                if (partnerGender == "Wanita") {
                    layoutPrediction.visibility = View.VISIBLE
                    loadAllPartnerLogs(partnerId, true)
                } else {
                    // Jika pasangan adalah pria, fitur prediksi dan fase tidak relevan
                    layoutPrediction.visibility = View.GONE
                    loadAllPartnerLogs(partnerId, false)
                }
            }
    }

    private fun loadAllPartnerLogs(partnerId: String, loadCycleData: Boolean) {
        datesWithLogs.clear()
        cycleStartDates.clear()

        firestore.collection("daily_logs").whereEqualTo("ownerId", partnerId).get()
            .addOnSuccessListener { logDocs ->
                for (doc in logDocs) {
                    datesWithLogs.add(LocalDate.parse(doc.getString("date"), dateFormatter))
                }
                if (loadCycleData) {
                    firestore.collection("cycle_logs").whereEqualTo("ownerId", partnerId).get()
                        .addOnSuccessListener { cycleDocs ->
                            for (doc in cycleDocs) {
                                val date = LocalDate.parse(doc.getString("startDate"), dateFormatter)
                                cycleStartDates.add(date)
                            }
                            setupCalendar(partnerId)
                            calculateAndDisplayPrediction(cycleStartDates.toList().sorted())
                        }
                } else {
                    setupCalendar(partnerId)
                }
            }
    }

    private fun setupCalendar(partnerId: String) {
        class DayViewContainer(view: View) : ViewContainer(view) {
            val textView: TextView = view.findViewById(R.id.calendar_day_text)
        }

        calendarView.dayBinder = object : MonthDayBinder<DayViewContainer> {
            override fun create(view: View) = DayViewContainer(view)
            override fun bind(container: DayViewContainer, day: CalendarDay) {
                container.textView.text = day.date.dayOfMonth.toString()
                if (day.position == DayPosition.MonthDate) {
                    container.view.visibility = View.VISIBLE

                    if (datesWithLogs.contains(day.date)) {
                        container.view.setOnClickListener { showDailyLogSheet(day.date, partnerId) }
                    } else {
                        container.view.setOnClickListener(null)
                    }

                    when {
                        cycleStartDates.contains(day.date) -> {
                            container.textView.setBackgroundResource(R.drawable.selected_day_bg)
                            container.textView.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white))
                        }
                        datesWithLogs.contains(day.date) -> {
                            container.textView.setBackgroundResource(R.drawable.daily_log_bg)
                            container.textView.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.black))
                        }
                        else -> {
                            container.textView.background = null
                            container.textView.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.black))
                        }
                    }
                } else {
                    container.view.visibility = View.INVISIBLE
                }
            }
        }

        val currentMonth = YearMonth.now()
        calendarView.setup(currentMonth.minusMonths(12), currentMonth.plusMonths(12), DayOfWeek.SUNDAY)
        calendarView.scrollToMonth(currentMonth)
        calendarView.monthScrollListener = { month ->
            val title = "${month.yearMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${month.yearMonth.year}"
            tvMonthYear.text = title
        }
    }

    private fun showDailyLogSheet(date: LocalDate, partnerId: String) {
        val dailyLogSheet = DailyLogBottomSheet(date, partnerId, isReadOnly = true) {}
        dailyLogSheet.show(parentFragmentManager, "PartnerDailyLogSheet")
    }

    private fun sendNotificationRequest(message: String) {
        val myUserId = auth.currentUser?.uid ?: return
        // Pastikan partnerId tidak null sebelum mengirim notifikasi
        partnerId?.let { pId ->
            val request = hashMapOf(
                "fromUserId" to myUserId,
                "toUserId" to pId, // Gunakan pId yang sudah pasti non-null
                "message" to message,
                "timestamp" to System.currentTimeMillis()
            )
            Log.d("PartnerMonitoring", "Mencoba mengirim notifikasi: $request")
            firestore.collection("notification_requests").add(request)
                .addOnSuccessListener {
                    Log.d("PartnerMonitoring", "Notifikasi berhasil dikirim. ID Dokumen: ${it.id}")
                    Toast.makeText(requireContext(), "Permintaan reminder terkirim!", Toast.LENGTH_SHORT).show()
                    etReminder.text.clear()
                }
                .addOnFailureListener { e ->
                    Log.e("PartnerMonitoring", "Gagal mengirim notifikasi", e)
                    Toast.makeText(requireContext(), "Gagal mengirim: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        } ?: run {
            Toast.makeText(requireContext(), "Pasangan tidak ditemukan untuk mengirim notifikasi.", Toast.LENGTH_SHORT).show()
            Log.w("PartnerMonitoring", "Gagal mengirim notifikasi: partnerId null")
        }
    }

    private fun calculateAndDisplayPrediction(dates: List<LocalDate>) {
        if (dates.size < 2) {
            tvPrediction.text = "Butuh lebih banyak data siklus"
            tvPhase.text = "Data Kurang"
            tvPhaseInfo.text = "Catat minimal 2 siklus untuk melihat info fase dan prediksi."
            return
        }
        val cycleDurations = (0 until dates.size - 1).map {
            java.time.temporal.ChronoUnit.DAYS.between(dates[it], dates[it + 1])
        }
        val averageDuration = cycleDurations.average().toLong()
        val lastDate = dates.last()
        val predictedDate = lastDate.plusDays(averageDuration)
        val displayFormatter = DateTimeFormatter.ofPattern("dd MMMM yyyy", Locale.getDefault())
        tvPrediction.text = predictedDate.format(displayFormatter)
        calculateAndDisplayCurrentPhase(dates.last(), averageDuration)
    }

    private fun calculateAndDisplayCurrentPhase(lastStartDate: LocalDate, avgCycleLength: Long) {
        val today = LocalDate.now()
        val dayOfCycle = java.time.temporal.ChronoUnit.DAYS.between(lastStartDate, today) + 1
        when {
            dayOfCycle in 1..5 -> {
                tvPhase.text = "Fase Menstruasi"
                tvPhaseInfo.text = "Energi sedang rendah. Waktunya istirahat & perbanyak asupan zat besi."
            }
            dayOfCycle in 6..14 -> {
                tvPhase.text = "Fase Folikular"
                tvPhaseInfo.text = "Energi mulai kembali! Mood membaik. Waktu yang baik untuk aktivitas produktif."
            }
            dayOfCycle in 15..17 -> {
                tvPhase.text = "Fase Ovulasi (Masa Subur)"
                tvPhaseInfo.text = "Puncak energi dan mood! Sangat sosial dan percaya diri."
            }
            dayOfCycle > 1 && dayOfCycle <= avgCycleLength -> {
                tvPhase.text = "Fase Luteal (PMS)"
                tvPhaseInfo.text = "Energi mulai menurun. Mungkin merasa lebih sensitif atau mudah lelah (PMS)."
            }
            else -> {
                tvPhase.text = "Siklus Selesai"
                tvPhaseInfo.text = "Menunggu siklus berikutnya dimulai."
            }
        }
    }
}
