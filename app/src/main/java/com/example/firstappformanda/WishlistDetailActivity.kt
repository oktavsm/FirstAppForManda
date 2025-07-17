package com.example.firstappformanda // <- SESUAIKAN

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import com.bumptech.glide.Glide
import com.google.android.material.button.MaterialButton
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.util.Date
import java.util.UUID

class WishlistDetailActivity : AppCompatActivity() {

    // --- Deklarasi Variabel UI & Sistem ---
    private lateinit var ivPhoto: ImageView
    private lateinit var btnUpload: MaterialButton
    private lateinit var etTitle: EditText
    private lateinit var etNotes: EditText
    private lateinit var btnComplete: MaterialButton
    private lateinit var btnSave: MaterialButton

    private lateinit var firestore: FirebaseFirestore
    private lateinit var storage: FirebaseStorage

    private var wishlistDocId: String? = null
    private var wishItemId: String? = null
    private var imageUri: Uri? = null
    private var existingWishItem: WishlistItem? = null
    private var oldItemMap: HashMap<String, Any>? = null // Untuk menyimpan item lama saat edit/complete

    // Launcher modern untuk memilih gambar dari galeri
    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            imageUri = result.data?.data
            // Tampilkan gambar yang dipilih
            Glide.with(this).load(imageUri).into(ivPhoto)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_wishlist_detail)

        initializeFirebase()
        initializeViews()

        // Ambil data dari Intent
        wishlistDocId = intent.getStringExtra("WISHLIST_DOC_ID")
        wishItemId = intent.getStringExtra("WISH_ITEM_ID")

        if (wishlistDocId == null) {
            Toast.makeText(this, "Error: ID Wishlist tidak ditemukan.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        if (wishItemId != null) {
            // Mode Edit: muat data yang sudah ada
            loadWishData()
        } else {
            // Mode Tambah Baru
            btnComplete.visibility = View.GONE
        }

        setupListeners()
    }

    private fun initializeFirebase() {
        firestore = FirebaseFirestore.getInstance()
        storage = FirebaseStorage.getInstance()
    }

    private fun initializeViews() {
        ivPhoto = findViewById(R.id.iv_detail_wish_photo)
        btnUpload = findViewById(R.id.btn_upload_photo)
        etTitle = findViewById(R.id.et_detail_wish_title)
        etNotes = findViewById(R.id.et_detail_wish_notes)
        btnComplete = findViewById(R.id.btn_complete_wish)
        btnSave = findViewById(R.id.btn_save_wish)
    }

    private fun setupListeners() {
        btnUpload.setOnClickListener {
            // Buka galeri HP
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            imagePickerLauncher.launch(intent)
        }
        btnSave.setOnClickListener {
            // Jika ada gambar baru yang dipilih, upload dulu
            if (imageUri != null) {
                uploadImageAndSaveWish()
            } else {
                // Jika tidak ada gambar baru, langsung simpan data teks
                saveWishData(existingWishItem?.imageUrl)
            }
        }
        btnComplete.setOnClickListener {
            // Logika untuk menandai selesai
            if (existingWishItem?.imageUrl.isNullOrEmpty()) {
                Toast.makeText(this, "Upload foto momen dulu untuk menandai selesai!", Toast.LENGTH_SHORT).show()
            } else {
                markAsCompleted()
            }
        }
    }

    private fun loadWishData() {
        firestore.collection("wishlists").document(wishlistDocId!!).get()
            .addOnSuccessListener { snapshot ->
                val items = snapshot.get("items") as? List<HashMap<String, Any>> ?: return@addOnSuccessListener
                val itemMap = items.find { it["id"] == wishItemId } ?: return@addOnSuccessListener
                oldItemMap = itemMap // Simpan item lama untuk proses update

                existingWishItem = WishlistItem(
                    id = itemMap["id"] as? String,
                    title = itemMap["title"] as? String,
                    notes = itemMap["notes"] as? String,
                    imageUrl = itemMap["imageUrl"] as? String,
                    isCompleted = itemMap["completed"] as? Boolean ?: false,
                    createdAt = (itemMap["createdAt"] as? com.google.firebase.Timestamp)?.toDate(),
                    completedDate = (itemMap["completedDate"] as? com.google.firebase.Timestamp)?.toDate()
                )

                etTitle.setText(existingWishItem?.title)
                etNotes.setText(existingWishItem?.notes)
                if (!existingWishItem?.imageUrl.isNullOrEmpty()) {
                    Glide.with(this).load(existingWishItem?.imageUrl).into(ivPhoto)
                }

                if (existingWishItem?.isCompleted == true) {
                    btnComplete.text = "Sudah Tercapai!"
                    btnComplete.isEnabled = false
                }
            }
    }

    private fun uploadImageAndSaveWish() {
        val fileName = "wish_${UUID.randomUUID()}.jpg"
        val storageRef = storage.reference.child("wishlist_images/$fileName")

        storageRef.putFile(imageUri!!)
            .addOnSuccessListener {
                storageRef.downloadUrl.addOnSuccessListener { uri ->
                    // Setelah URL didapat, baru simpan semua data
                    saveWishData(uri.toString())
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Gagal upload foto.", Toast.LENGTH_SHORT).show()
            }
    }

    private fun saveWishData(imageUrl: String?) {
        val title = etTitle.text.toString().trim()
        if (title.isEmpty()) {
            Toast.makeText(this, "Judul tidak boleh kosong.", Toast.LENGTH_SHORT).show()
            return
        }

        if (wishItemId == null) {
            // --- Mode Tambah Baru ---
            val newWish = WishlistItem(
                id = UUID.randomUUID().toString(),
                title = title,
                notes = etNotes.text.toString().trim(),
                imageUrl = imageUrl,
                isCompleted = false,
                createdAt = Date()
            )
            // Gunakan FieldValue.arrayUnion untuk menambahkan item baru ke array
            firestore.collection("wishlists").document(wishlistDocId!!)
                .update("items", FieldValue.arrayUnion(newWish))
                .addOnSuccessListener { finish() }
        } else {
            // --- Mode Edit ---
            // Ini bagian yang paling tricky: kita harus hapus yang lama, lalu tambah yang baru
            firestore.collection("wishlists").document(wishlistDocId!!).get()
                .addOnSuccessListener {
                    // oldItemMap sudah diisi di loadWishData()

                    val updatedWish = existingWishItem!!.copy(
                        title = title,
                        notes = etNotes.text.toString().trim(),
                        imageUrl = imageUrl ?: existingWishItem?.imageUrl // Pakai URL baru jika ada
                    )

                    val batch = firestore.batch()
                    val docRef = firestore.collection("wishlists").document(wishlistDocId!!)
                    if (oldItemMap != null) batch.update(docRef, "items", FieldValue.arrayRemove(oldItemMap))
                    batch.update(docRef, "items", FieldValue.arrayUnion(updatedWish))

                    batch.commit().addOnSuccessListener { finish() }
                }
        }
    }

    private fun markAsCompleted() {
        // Logika ini mirip dengan edit, tapi kita hanya update status 'isCompleted'
        val updatedWish = existingWishItem!!.copy(
            isCompleted = true,
            completedDate = Date()
        )

        // Pastikan oldItemMap tidak null sebelum melakukan operasi batch
        if (oldItemMap == null) {
            Toast.makeText(this, "Error: Data impian lama tidak ditemukan.", Toast.LENGTH_SHORT).show()
            return
        }

        firestore.collection("wishlists").document(wishlistDocId!!)
            .update("items", FieldValue.arrayRemove(oldItemMap!!)) // Hapus item lama
            .addOnSuccessListener {
                firestore.collection("wishlists").document(wishlistDocId!!)
                    .update("items", FieldValue.arrayUnion(updatedWish)) // Tambah item yang sudah diupdate
                    .addOnSuccessListener {
                        Toast.makeText(this, "Selamat! Impian tercapai!", Toast.LENGTH_LONG).show()
                        finish()
                    }
            }
    }
}