package com.example.firstappformanda // <- SESUAIKAN

import com.google.firebase.firestore.PropertyName

data class ActivityModel(
    val id: String? = null,
    var type: String? = null, // "event" atau "task"
    var title: String? = null,
    var notes: String? = null,
    var ownerId: String? = null,

    @get:PropertyName("isDone")
    var isDone: Boolean? = false,

    var startTime: Long? = null,
    var endTime: Long? = null,

    var googleEventId: String? = null // <-- TAMBAHAN BARU
) {
    // Constructor kosong, tambahkan null untuk properti baru
    constructor() : this(null, null, null, null, null, false, null, null, null)
}
