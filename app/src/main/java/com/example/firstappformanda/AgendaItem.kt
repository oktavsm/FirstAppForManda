package com.example.firstappformanda // <- SESUAIKAN

// Data class ini mewakili satu baris di dasbor "Hari Ini"
data class AgendaItem(
    val timestamp: Long,      // Waktu mulai (untuk sorting)
    val title: String,
    val subtitle: String,
    val type: String          // "event" atau "task"
)
