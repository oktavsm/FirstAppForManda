package com.example.firstappformanda

data class Task(
    val id: String? = null, // ID dari dokumen Firestore
    val title: String? = null,
    val isDone: Boolean = false,
    val ownerId: String? = null
)