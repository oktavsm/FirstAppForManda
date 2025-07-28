package com.example.firstappformanda // <- SESUAIKAN

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.Menu
import android.util.Log // Import Log
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.app.ActivityCompat // Import untuk requestPermissions
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging

class MainActivity : AppCompatActivity() {

    private val TAG = "MainActivity" // Tag untuk Logcat
    // --- Deklarasi Variabel ---
    private lateinit var topAppBar: MaterialToolbar
    private lateinit var bottomNavigation: BottomNavigationView
    private lateinit var auth: FirebaseAuth // Mengganti nama variabel agar konsisten
    private lateinit var firestore: FirebaseFirestore
    private lateinit var googleSignInClient: GoogleSignInClient

    private var hasPartner: Boolean = false // Untuk melacak status hubungan

    // Konstanta untuk kode permintaan izin
    private val REQUEST_CALENDAR_PERMISSIONS = 123

    // (BARU) Launcher untuk meminta izin kalender
    private val requestCalendarPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            // Tindakan setelah izin diberikan atau ditolak bisa ditambahkan di sini jika perlu
            // Misalnya, menampilkan pesan jika izin ditolak
        }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Inisialisasi
        initializeFirebase()
        initializeViews()
        configureGoogleSignIn()

        // (BARU) Minta izin kalender jika belum ada
        requestCalendarPermissionIfNeeded()

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

    // (BARU) Fungsi untuk meminta izin kalender jika belum diberikan
    // (MODIFIKASI) Selalu minta akses izin ke Google Calendar API jika belum ada
    private fun requestCalendarPermissionIfNeeded() {
        val hasReadCalendarPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED
        val hasWriteCalendarPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_CALENDAR) == PackageManager.PERMISSION_GRANTED

        if (!hasReadCalendarPermission || !hasWriteCalendarPermission) {
            // Jika salah satu atau kedua izin belum diberikan, minta keduanya.
            // Anda bisa meminta satu per satu jika ingin penanganan yang lebih spesifik.
            // Di sini, kita akan meminta keduanya sekaligus.
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR),
                REQUEST_CALENDAR_PERMISSIONS
            )
            // Atau, jika Anda ingin menggunakan ActivityResultLauncher untuk satu izin saja (misal WRITE_CALENDAR yang biasanya sudah mencakup READ):
            // requestCalendarPermissionLauncher.launch(Manifest.permission.WRITE_CALENDAR)
        }}

    private fun initializeViews() {
        topAppBar = findViewById(R.id.top_app_bar)
        bottomNavigation = findViewById(R.id.bottom_navigation)
        bottomNavigation.itemIconTintList = null // Agar ikon SVG tampil dengan warna asli
    }

    private fun configureGoogleSignIn() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            // (BARU) Tambahkan scope untuk Google Calendar API
            .requestScopes(com.google.android.gms.common.api.Scope("https://www.googleapis.com/auth/calendar"))
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)
    }

    private fun checkUserStatus() {
        if (auth.currentUser == null) { // Menggunakan nama variabel baru
            navigateToAuth()
        } else {
            fetchUserData()
            checkAndRefreshFcmToken() // Panggil fungsi cek FCM token
        }
    }
    private fun fetchUserData() {
        val userId = auth.currentUser?.uid ?: return // Menggunakan nama variabel baru
        firestore.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    // (BARU) Cek apakah gender sudah dipilih
                    val gender = document.getString("gender")
                    if (gender.isNullOrEmpty()) {
                        // Jika belum, arahkan ke GenderSelectionActivity
                        startActivity(Intent(this, GenderSelectionActivity::class.java))
                        finishAffinity() // Tutup semua activity sebelumnya
                        return@addOnSuccessListener // Hentikan eksekusi lebih lanjut
                    }
                    val coupleId = document.getString("coupleId") // Pindahkan ini setelah pengecekan gender
                    hasPartner = !coupleId.isNullOrEmpty() // Pindahkan ini setelah pengecekan gender
                    invalidateOptionsMenu() // Perintahkan menu untuk gambar ulang dirinya
                }
            }
    }

    // (BARU) Fungsi untuk cek FCM token dan refresh ke Firestore
    private fun checkAndRefreshFcmToken() {
        val userId = auth.currentUser?.uid ?: return
        val userDocRef = firestore.collection("users").document(userId)

        userDocRef.get().addOnSuccessListener { document ->
            if (document != null && document.exists()) {
                val currentFcmToken = document.getString("fcmToken")
                Log.d(TAG, "Current FCM Token from Firestore: $currentFcmToken")
                FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val newToken = task.result
                        Log.d(TAG, "New FCM Token from FirebaseMessaging: $newToken")
                        // Perbarui token jika belum ada atau berbeda
                        if (currentFcmToken.isNullOrEmpty() || currentFcmToken != newToken) {
                            Log.d(TAG, "FCM Token is null, empty, or different. Updating Firestore.")
                            userDocRef.update("fcmToken", newToken)
                                .addOnSuccessListener {
                                    Log.d(TAG, "FCM token successfully updated in Firestore.")
                                }
                                .addOnFailureListener { e ->
                                    Log.e(TAG, "Failed to update FCM token in Firestore.", e)
                                }
                        } else {
                            Log.d(TAG, "FCM Token is already up-to-date in Firestore.")
                        }
                    } else {
                        Log.w(TAG, "Fetching FCM registration token failed", task.exception)
                    }
                }
            } else {
                Log.d(TAG, "User document does not exist for FCM token check.")
            }
        }.addOnFailureListener { e ->
            Log.e(TAG, "Error getting user document for FCM token check", e)
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

    // (BARU) Override untuk menangani hasil permintaan izin runtime
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            REQUEST_CALENDAR_PERMISSIONS -> {
                if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                    // Izin diberikan
                    Log.d(TAG, "Izin kalender diberikan.")
                    // Anda bisa melanjutkan dengan fungsionalitas kalender di sini jika perlu
                } else {
                    // Izin ditolak
                    Log.d(TAG, "Izin kalender ditolak.")
                    Toast.makeText(this, "Izin kalender dibutuhkan untuk fitur tertentu.", Toast.LENGTH_LONG).show()
                }
            }
        }    }
}
