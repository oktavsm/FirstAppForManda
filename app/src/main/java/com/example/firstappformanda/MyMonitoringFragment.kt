package com.example.firstappformanda // <- SESUAIKAN

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
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

class MyMonitoringFragment : Fragment() {

    // --- Deklarasi Variabel ---
    private lateinit var calendarView: CalendarView
    private lateinit var tvMonthYear: TextView
    private lateinit var layoutPrediction: LinearLayout
    private lateinit var tvPrediction: TextView
    private lateinit var tvPhase: TextView
    private lateinit var tvPhaseInfo: TextView
    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    private var currentUserGender: String? = null
    private val cycleStartDates = mutableSetOf<LocalDate>()
    private val datesWithLogs = mutableSetOf<LocalDate>()
    private val dateToCycleDocIdMap = mutableMapOf<LocalDate, String>()
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_my_monitoring, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        firestore = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        initializeViews(view)
        loadUserDataAndSetup()
    }

    private fun initializeViews(view: View) {
        tvMonthYear = view.findViewById(R.id.tv_month_year)
        calendarView = view.findViewById(R.id.calendar_view)
        layoutPrediction = view.findViewById(R.id.layout_prediction_and_phase)
        tvPrediction = view.findViewById(R.id.tv_my_prediction)
        tvPhase = view.findViewById(R.id.tv_my_current_phase)
        tvPhaseInfo = view.findViewById(R.id.tv_my_phase_info)

        view.findViewById<ImageView>(R.id.iv_calendar_forward).setOnClickListener {
            calendarView.findFirstVisibleMonth()?.let { calendarView.smoothScrollToMonth(it.yearMonth.plusMonths(1)) }
        }
        view.findViewById<ImageView>(R.id.iv_calendar_back).setOnClickListener {
            calendarView.findFirstVisibleMonth()?.let { calendarView.smoothScrollToMonth(it.yearMonth.minusMonths(1)) }
        }
    }

    private fun loadUserDataAndSetup() {
        val userId = auth.currentUser?.uid ?: return
        firestore.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    currentUserGender = document.getString("gender")
                    val loadCycle = currentUserGender == "Wanita"

                    if (loadCycle) {
                        layoutPrediction.visibility = View.VISIBLE
                    } else {
                        layoutPrediction.visibility = View.GONE
                    }
                    loadAllLogs(userId, loadCycle)
                }
            }
    }

    private fun loadAllLogs(userId: String, loadCycleData: Boolean) {
        datesWithLogs.clear()
        cycleStartDates.clear()
        dateToCycleDocIdMap.clear()

        firestore.collection("daily_logs").whereEqualTo("ownerId", userId).get()
            .addOnSuccessListener { logDocs ->
                for (doc in logDocs) {
                    datesWithLogs.add(LocalDate.parse(doc.getString("date"), dateFormatter))
                }
                if (loadCycleData) {
                    firestore.collection("cycle_logs").whereEqualTo("ownerId", userId).get()
                        .addOnSuccessListener { cycleDocs ->
                            for (doc in cycleDocs) {
                                val date = LocalDate.parse(doc.getString("startDate"), dateFormatter)
                                cycleStartDates.add(date)
                                dateToCycleDocIdMap[date] = doc.id
                            }
                            setupCalendar()
                            calculateAndDisplayPrediction(cycleStartDates.toList().sorted())
                        }
                } else {
                    setupCalendar()
                }
            }
    }

    private fun setupCalendar() {
        class DayViewContainer(view: View) : ViewContainer(view) {
            val textView: TextView = view.findViewById(R.id.calendar_day_text)
        }

        calendarView.dayBinder = object : MonthDayBinder<DayViewContainer> {
            override fun create(view: View) = DayViewContainer(view)
            override fun bind(container: DayViewContainer, day: CalendarDay) {
                container.textView.text = day.date.dayOfMonth.toString()
                if (day.position == DayPosition.MonthDate) {
                    container.view.visibility = View.VISIBLE
                    container.view.setOnClickListener { handleDateClick(day.date) }
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

    private fun handleDateClick(date: LocalDate) {
        if (currentUserGender == "Wanita") {
            val options = arrayOf("Tandai sebagai Awal Haid", "Isi Catatan Harian")
            AlertDialog.Builder(requireContext())
                .setTitle("Pilih Aksi untuk ${date.format(DateTimeFormatter.ofPattern("dd MMMM"))}")
                .setItems(options) { _, which ->
                    when (which) {
                        0 -> saveOrDeleteCycleDate(date)
                        1 -> showDailyLogSheet(date)
                    }
                }
                .show()
        } else {
            showDailyLogSheet(date)
        }
    }

    private fun saveOrDeleteCycleDate(date: LocalDate) {
        val userId = auth.currentUser?.uid ?: return
        val dateString = date.format(dateFormatter)

        if (cycleStartDates.contains(date)) {
            val docId = dateToCycleDocIdMap[date]
            if (docId != null) {
                firestore.collection("cycle_logs").document(docId).delete().addOnSuccessListener {
                    loadAllLogs(userId, true)
                }
            }
        } else {
            val cycleLog = hashMapOf("ownerId" to userId, "startDate" to dateString)
            firestore.collection("cycle_logs").add(cycleLog).addOnSuccessListener {
                loadAllLogs(userId, true)
            }
        }
    }

    private fun showDailyLogSheet(date: LocalDate) {
        val myUserId = auth.currentUser?.uid ?: return
        val dailyLogSheet = DailyLogBottomSheet(date, myUserId, isReadOnly = false) {
            loadAllLogs(myUserId, currentUserGender == "Wanita")
        }
        dailyLogSheet.show(parentFragmentManager, "DailyLogBottomSheet")
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
                tvPhase.text = "Fase Ovulasi"
                tvPhaseInfo.text = "Puncak energi dan mood! Sangat sosial dan percaya diri."
            }
            dayOfCycle > 18 && dayOfCycle <= avgCycleLength -> {
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
