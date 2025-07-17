package com.example.firstappformanda

data class Message(
    val senderId: String? = null,
    val text: String? = null,
    val timestamp: Long? = null
) {
    // Constructor kosong wajib untuk Firestore
    constructor() : this(null, null, null)
}