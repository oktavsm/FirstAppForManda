package com.example.firstappformanda // <- SESUAIKAN

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class WishlistItem(
    val id: String? = null,
    var title: String? = null,
    var notes: String? = null,
    var imageUrl: String? = null, // URL foto momen
    var isCompleted: Boolean = false,
    @ServerTimestamp
    val createdAt: Date? = null, // Waktu dibuat
    var completedDate: Date? = null // Waktu diselesaikan
) {
    // Constructor kosong wajib untuk Firestore
    constructor() : this(null, null, null, null, false, null, null)
}
