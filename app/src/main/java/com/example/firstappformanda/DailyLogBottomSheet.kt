package com.example.firstappformanda // <- SESUAIKAN

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.view.children
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

// Class ini harus mewarisi BottomSheetDialogFragment
class DailyLogBottomSheet(
    private val date: LocalDate,
    private val ownerId: String,
    private val isReadOnly: Boolean, // <-- TAMBAHAN BARU
    private val onLogChanged: () -> Unit // Callback saat data berubah (disimpan/dihapus)
) : BottomSheetDialogFragment() {


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        // Hubungkan dengan layout XML yang kita buat
        return inflater.inflate(R.layout.bottom_sheet_daily_log, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val firestore = FirebaseFirestore.getInstance()
        // Format tanggal untuk ditampilkan dan untuk ID dokumen
        val displayFormatter = DateTimeFormatter.ofPattern("dd MMMM yyyy", Locale.getDefault())
        val docIdFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val dateStringForDocId = date.format(docIdFormatter)
        val docId = "${ownerId}_$dateStringForDocId"
        val docRef = firestore.collection("daily_logs").document(docId)

        // Hubungkan UI
        val tvSelectedDate: TextView = view.findViewById(R.id.tv_selected_date)
        val chipGroupMoods: ChipGroup = view.findViewById(R.id.chip_group_moods)
        val chipGroupSymptoms: ChipGroup = view.findViewById(R.id.chip_group_symptoms)
        val etNotes: EditText = view.findViewById(R.id.et_daily_notes)
        val btnSave: Button = view.findViewById(R.id.btn_save_daily_log)
        val btnDelete: Button = view.findViewById(R.id.btn_delete_daily_log) // <-- Tombol baru

        tvSelectedDate.text = if(isReadOnly) "Catatan Pasangan" else "Catatan untuk ${date.format(displayFormatter)}"

        // Muat data yang sudah ada untuk tanggal ini (jika ada)
        docRef.get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    // Jika dokumen ada, tampilkan tombol hapus (jika tidak read-only)
                    if (!isReadOnly) btnDelete.visibility = View.VISIBLE

                    etNotes.setText(document.getString("notes"))
                    val savedMood = document.getString("mood")
                    for (chip in chipGroupMoods.children) {
                        if ((chip as Chip).text == savedMood) {
                            chip.isChecked = true
                            break
                        }
                    }
                    val savedSymptoms = document.get("symptoms") as? List<String> ?: listOf()
                    for (chip in chipGroupSymptoms.children) {
                        if (savedSymptoms.contains((chip as Chip).text.toString())) {
                            chip.isChecked = true
                        }
                    }
                }
            }
        if (isReadOnly) {
            // Jika mode "hanya lihat"
            etNotes.isEnabled = false // Matikan EditText
            btnSave.visibility = View.GONE // Sembunyikan tombol simpan
            btnDelete.visibility = View.GONE // Pastikan tombol hapus juga disembunyikan

            // Matikan semua chip agar tidak bisa diklik
            for (chip in chipGroupMoods.children) { (chip as Chip).isClickable = false }
            for (chip in chipGroupSymptoms.children) { (chip as Chip).isClickable = false }
        } else {
            // Pasang listener hanya jika bisa diedit



            btnSave.setOnClickListener {
                // Ambil data dari UI
                val selectedMoodChipId = chipGroupMoods.checkedChipId
                val mood =
                    if (selectedMoodChipId != View.NO_ID) view.findViewById<Chip>(selectedMoodChipId).text.toString() else null

                val symptoms = chipGroupSymptoms.children
                    .filter { (it as Chip).isChecked }
                    .map { (it as Chip).text.toString() }
                    .toList()

                val notes = etNotes.text.toString()

                val dailyLog = hashMapOf(
                    "date" to dateStringForDocId,
                    "ownerId" to ownerId,
                    "mood" to mood,
                    "symptoms" to symptoms,
                    "notes" to notes
                )

                // Simpan ke Firestore
                docRef
                    .set(
                        dailyLog,
                        SetOptions.merge()
                    ) // SetOptions.merge() agar tidak menimpa data lain
                    .addOnSuccessListener {
                        Toast.makeText(context, "Catatan berhasil disimpan", Toast.LENGTH_SHORT)
                            .show()
                        onLogChanged() // Panggil callback untuk memberitahu activity bahwa data disimpan
                        dismiss() // Tutup bottom sheet
                    }
            }

            btnDelete.setOnClickListener {
                showDeleteConfirmation(docRef)
            }

        }
    }
    private fun showDeleteConfirmation(docRef: com.google.firebase.firestore.DocumentReference) {
        AlertDialog.Builder(requireContext())
            .setTitle("Hapus Catatan?")
            .setMessage("Yakin ingin menghapus catatan harian ini?")
            .setPositiveButton("Ya, Hapus") { _, _ ->
                docRef.delete()
                    .addOnSuccessListener {
                        Toast.makeText(context, "Catatan berhasil dihapus", Toast.LENGTH_SHORT).show()
                        onLogChanged() // Panggil callback untuk refresh kalender
                        dismiss()
                    }
                    .addOnFailureListener {
                        Toast.makeText(context, "Gagal menghapus catatan", Toast.LENGTH_SHORT).show()
                    }
            }
            .setNegativeButton("Batal", null)
            .show()
    }
}

