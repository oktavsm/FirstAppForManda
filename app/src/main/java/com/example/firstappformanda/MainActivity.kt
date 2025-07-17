package com.example.firstappformanda // <- SESUAIKAN

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : AppCompatActivity() {

    // --- Deklarasi Variabel ---
    private lateinit var topAppBar: MaterialToolbar
    private lateinit var bottomNavigation: BottomNavigationView
    private lateinit var auth: FirebaseAuth // Mengganti nama variabel agar konsisten
    private lateinit var firestore: FirebaseFirestore
    private lateinit var googleSignInClient: GoogleSignInClient

    private var hasPartner: Boolean = false // Untuk melacak status hubungan

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Inisialisasi
        initializeFirebase()
        initializeViews()
        configureGoogleSignIn()

        // Atur Toolbar sebagai Action Bar aplikasi
        setSupportActionBar(topAppBar)

        // Atur listener untuk navigasi bawah
        bottomNavigation.setOnItemSelectedListener { item ->
            var selectedFragment: Fragment? = null
            var title = ""

            when (item.itemId) {
                R.id.navigation_dashboard -> {
                    selectedFragment = DashboardFragment()
                    title = "Dasbor Hari Ini"
                }
                R.id.navigation_monitoring -> {
                    selectedFragment = MonitoringContainerFragment()
                    title = "Monitoring"
                }
                R.id.navigation_productivity -> {
                    selectedFragment = ProductivityContainerFragment()
                    title = "Produktivitas"
                }
                R.id.navigation_chat -> {
                    selectedFragment = ChatFragment()
                    title = "Chat"
                }
            }

            if (selectedFragment != null) {
                loadFragment(selectedFragment)
                topAppBar.title = title
            }
            true
        }

        // Tampilkan halaman default (Dasbor) saat pertama kali dibuka
        if (savedInstanceState == null) {
            bottomNavigation.selectedItemId = R.id.navigation_dashboard
        }
    }

    override fun onStart() {
        super.onStart()
        checkUserStatus()
    }

    private fun initializeFirebase() {
        auth = FirebaseAuth.getInstance() // Menggunakan nama variabel baru
        firestore = FirebaseFirestore.getInstance()
    }

    private fun initializeViews() {
        topAppBar = findViewById(R.id.top_app_bar)
        bottomNavigation = findViewById(R.id.bottom_navigation)
        bottomNavigation.itemIconTintList = null // Agar ikon SVG tampil dengan warna asli
    }

    private fun configureGoogleSignIn() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)
    }

    private fun checkUserStatus() {
        if (auth.currentUser == null) { // Menggunakan nama variabel baru
            navigateToAuth()
        } else {
            fetchUserData()
        }
    }
    private fun fetchUserData() {
        val userId = auth.currentUser?.uid ?: return // Menggunakan nama variabel baru
        firestore.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val coupleId = document.getString("coupleId")
                    hasPartner = !coupleId.isNullOrEmpty()
                    invalidateOptionsMenu() // Perintahkan menu untuk gambar ulang dirinya
                }
            }
    }
    // (BARU) Launcher untuk menerima sinyal dari CoupleActivity
    private val coupleActivityResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // Jika ada perubahan (berhasil terhubung), muat ulang data user
            // dan refresh halaman dashboard
            bottomNavigation.selectedItemId = R.id.navigation_dashboard
            fetchUserData()
        }
    }

    // --- Fungsi untuk membuat menu tiga titik ---
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    // --- Fungsi untuk mengatur menu sebelum ditampilkan ---
    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        val coupleMenuItem = menu?.findItem(R.id.action_couple)
        if (hasPartner) {
            coupleMenuItem?.title = "Putuskan Koneksi"
        } else {
            coupleMenuItem?.title = "Hubungkan Pasangan"
        }
        return super.onPrepareOptionsMenu(menu)
    }

    // --- Fungsi untuk menangani klik menu ---
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_couple -> {
                if (hasPartner) {
                    showDisconnectConfirmationDialog()
                } else {
                    // Gunakan launcher baru saat memulai activity
                    val intent = Intent(this, CoupleActivity::class.java)
                    coupleActivityResultLauncher.launch(intent)
                }
                true
            }
            R.id.action_logout -> {
                auth.signOut() // Menggunakan nama variabel baru
                googleSignInClient.signOut().addOnCompleteListener {
                    navigateToAuth()
                }
                true
            }

            R.id.action_wishlist -> {
                startActivity(Intent(this, WishlistActivity::class.java))
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    // --- TAMBAHKAN DUA FUNGSI BARU INI ---

    private fun showDisconnectConfirmationDialog() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Putuskan Koneksi?")
            .setMessage("Yakin ingin memutuskan koneksi dengan pasanganmu?")
            .setPositiveButton("Ya, Putuskan") { _, _ ->
                disconnectPartner()
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun disconnectPartner() {
        val myUserId = auth.currentUser?.uid ?: return

        firestore.collection("users").document(myUserId).get()
            .addOnSuccessListener { myDocument ->
                val partnerId = myDocument.getString("coupleId")
                if (partnerId.isNullOrEmpty()) return@addOnSuccessListener
                val batch = firestore.batch()
                // Hapus data di dokumen kita
                val myDocRef = firestore.collection("users").document(myUserId)
                batch.update(myDocRef, "coupleId", "", "anniversaryDate", null)
                // Hapus data di dokumen pasangan
                val partnerDocRef = firestore.collection("users").document(partnerId)
                batch.update(partnerDocRef, "coupleId", "", "anniversaryDate", null)
                batch.commit().addOnSuccessListener {
                    Toast.makeText(this, "Koneksi berhasil diputuskan.", Toast.LENGTH_SHORT).show()
                    // Muat ulang semua data dasbor untuk refresh tampilan
                    fetchUserData() // Untuk update menu
                    bottomNavigation.selectedItemId = R.id.navigation_dashboard // Kembali ke dashboard untuk refresh
                }
            }
    }
    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }

    private fun navigateToAuth() {
        val intent = Intent(this, AuthActivity::class.java)
        startActivity(intent)
        finishAffinity()
    }
}
