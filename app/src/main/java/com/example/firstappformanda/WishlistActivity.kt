package com.example.firstappformanda // <- SESUAIKAN

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObject

class WishlistActivity : AppCompatActivity() {

    private lateinit var rvWishlist: RecyclerView
    private lateinit var fabAddWish: FloatingActionButton
    private lateinit var wishlistAdapter: WishlistAdapter
    private val wishList = mutableListOf<WishlistItem>()

    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    private var wishlistDocId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_wishlist)

        // Inisialisasi
        firestore = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()
        initializeViews()

        // Setup Adapter dengan listener-nya
        wishlistAdapter = WishlistAdapter(
            wishlist = wishList,
            onWishClickListener = { wish ->
                // Aksi saat bingkai foto di-klik
                val intent = Intent(this, WishlistDetailActivity::class.java)
                intent.putExtra("WISHLIST_DOC_ID", wishlistDocId)
                intent.putExtra("WISH_ITEM_ID", wish.id) // Kirim ID item spesifik
                startActivity(intent)
            },
            onDeleteClickListener = { wish ->
                // Aksi saat tombol hapus di-klik
                showDeleteConfirmation(wish)
            }
        )

        // Setup RecyclerView dengan 2 kolom
        rvWishlist.layoutManager = GridLayoutManager(this, 2)
        rvWishlist.adapter = wishlistAdapter

        // Mulai proses
        setupWishlistRoom()

        fabAddWish.setOnClickListener {
            if (wishlistDocId != null) {
                val intent = Intent(this, WishlistDetailActivity::class.java)
                intent.putExtra("WISHLIST_DOC_ID", wishlistDocId)
                // Tidak mengirim WISH_ITEM_ID berarti ini mode "Tambah Baru"
                startActivity(intent)
            } else {
                Toast.makeText(this, "Gagal mendapatkan ID wishlist.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // (BARU) Muat ulang data setiap kali halaman ini kembali terlihat
    override fun onResume() {
        super.onResume()
        // Jika wishlistDocId sudah ada, dengarkan lagi perubahannya
        if (wishlistDocId != null) {
            listenForWishes()
        }
    }

    private fun initializeViews() {
        rvWishlist = findViewById(R.id.rv_wishlist)
        fabAddWish = findViewById(R.id.fab_add_wish)
    }

    private fun setupWishlistRoom() {
        val myUserId = auth.currentUser?.uid ?: return
        firestore.collection("users").document(myUserId).get()
            .addOnSuccessListener { document ->
                val partnerId = document.getString("coupleId")
                if (partnerId.isNullOrEmpty()) {
                    Toast.makeText(this, "Kamu belum terhubung dengan pasangan", Toast.LENGTH_SHORT).show()
                    finish()
                    return@addOnSuccessListener
                }

                wishlistDocId = if (myUserId < partnerId) "${myUserId}_${partnerId}" else "${partnerId}_${myUserId}"
                listenForWishes()
            }
    }

    private fun listenForWishes() {
        if (wishlistDocId == null) return

        // Dengarkan perubahan pada dokumen wishlist pasangan ini
        firestore.collection("wishlists").document(wishlistDocId!!)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.w("WishlistActivity", "Listen failed.", e)
                    return@addSnapshotListener
                }

                if (snapshot != null && snapshot.exists()) {
                    // Ambil array 'items' dari dokumen
                    val items = snapshot.get("items") as? List<HashMap<String, Any>> ?: listOf()

                    wishList.clear()
                    // Ubah setiap item di array menjadi objek WishlistItem
                    items.forEach { itemMap ->
                        // Proses konversi manual
                        val wish = WishlistItem(
                            id = itemMap["id"] as? String,
                            title = itemMap["title"] as? String,
                            notes = itemMap["notes"] as? String,
                            imageUrl = itemMap["imageUrl"] as? String,
                            isCompleted = itemMap["completed"] as? Boolean ?: false,
                            createdAt = (itemMap["createdAt"] as? com.google.firebase.Timestamp)?.toDate(),
                            completedDate = (itemMap["completedDate"] as? com.google.firebase.Timestamp)?.toDate()
                        )
                        wishList.add(wish)
                    }
                    // Urutkan berdasarkan yang terbaru dibuat
                    wishList.sortByDescending { it.createdAt }
                    wishlistAdapter.notifyDataSetChanged()
                } else {
                    // Jika dokumen belum ada, buat dokumen baru dengan array kosong
                    firestore.collection("wishlists").document(wishlistDocId!!)
                        .set(hashMapOf("items" to listOf<WishlistItem>()))
                }
            }
    }

    private fun showDeleteConfirmation(wish: WishlistItem) {
        AlertDialog.Builder(this)
            .setTitle("Hapus Wishlist?")
            .setMessage("Yakin ingin menghapus '${wish.title}'?")
            .setPositiveButton("Ya, Hapus") { _, _ ->
                deleteWish(wish)
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun deleteWish(wish: WishlistItem) {
        if (wishlistDocId == null) return

        // Buat objek map dari item yang akan dihapus untuk dicocokkan di server
        val wishMap = mapOf(
            "id" to wish.id,
            "title" to wish.title,
            "notes" to wish.notes,
            "imageUrl" to wish.imageUrl,
            "completed" to wish.isCompleted,
            "createdAt" to wish.createdAt,
            "completedDate" to wish.completedDate
        )

        // Gunakan FieldValue.arrayRemove untuk menghapus elemen dari array
        firestore.collection("wishlists").document(wishlistDocId!!)
            .update("items", FieldValue.arrayRemove(wishMap))
            .addOnSuccessListener {
                Toast.makeText(this, "'${wish.title}' dihapus.", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Gagal menghapus: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}
