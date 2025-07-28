package com.example.firstappformanda // <- SESUAIKAN

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.SignInButton
import com.google.android.gms.common.api.ApiException
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.messaging.FirebaseMessaging

class RegisterFragment : Fragment() {

    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var googleSignInClient: GoogleSignInClient

    private lateinit var etName: EditText
    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnRegister: MaterialButton
    private lateinit var btnGoogleSignIn: SignInButton

    // Launcher untuk menangani hasil dari Google Sign-In
    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)!!
                firebaseAuthWithGoogle(account)
            } catch (e: ApiException) {
                Log.w("GoogleSignIn", "Google sign in failed in RegisterFragment", e)
                Toast.makeText(requireContext(), "Login Google Gagal", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_register, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Inisialisasi
        firebaseAuth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        initializeViews(view)
        configureGoogleSignIn()

        // Listeners
        btnRegister.setOnClickListener { registerWithEmail() }
        btnGoogleSignIn.setOnClickListener { signInWithGoogle() }
    }

    private fun initializeViews(view: View) {
        etName = view.findViewById(R.id.et_register_name)
        etEmail = view.findViewById(R.id.et_register_email)
        etPassword = view.findViewById(R.id.et_register_password)
        btnRegister = view.findViewById(R.id.btn_register)
        btnGoogleSignIn = view.findViewById(R.id.btn_google_sign_in_register)
    }

    private fun configureGoogleSignIn() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(requireActivity(), gso)
    }

    private fun registerWithEmail() {
        val name = etName.text.toString().trim()
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString().trim()

        if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
            Toast.makeText(requireContext(), "Semua kolom harus diisi!", Toast.LENGTH_SHORT).show()
            return
        }

        firebaseAuth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    createNewUserDocument(task.result.user!!, name, email)
                } else {
                    Toast.makeText(requireContext(), "Registrasi Gagal: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun signInWithGoogle() {
        googleSignInClient.signOut().addOnCompleteListener { // Selalu sign out dulu untuk memastikan bisa pilih akun
            val signInIntent = googleSignInClient.signInIntent
            googleSignInLauncher.launch(signInIntent)
        }
    }

    private fun firebaseAuthWithGoogle(account: GoogleSignInAccount) {
        val credential = GoogleAuthProvider.getCredential(account.idToken!!, null)
        firebaseAuth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val firebaseUser = task.result.user!!
                    val userDocRef = firestore.collection("users").document(firebaseUser.uid)

                    userDocRef.get().addOnSuccessListener { documentSnapshot ->
                        if (!documentSnapshot.exists()) {
                            createNewUserDocument(firebaseUser, firebaseUser.displayName ?: "", firebaseUser.email ?: "")
                        } else {
                            // Jika user sudah ada tapi mencoba daftar lagi, anggap sebagai login
                            checkUserProfile(firebaseUser)
                        }
                    }
                } else {
                    Toast.makeText(requireContext(), "Autentikasi Firebase Gagal", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun createNewUserDocument(firebaseUser: FirebaseUser, name: String, email: String) {
        val userId = firebaseUser.uid
        val userMap = hashMapOf(
            "userId" to userId,
            "nama" to name,
            "email" to email,
            "gender" to "",
            "coupleId" to ""
        )

        firestore.collection("users").document(userId).set(userMap)
            .addOnSuccessListener {
                saveFcmTokenAndNavigate(userId, GenderSelectionActivity::class.java)
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Gagal membuat profil.", Toast.LENGTH_SHORT).show()
            }
    }

    // Fungsi ini akan dipanggil dari LoginFragment juga
    fun checkUserProfile(firebaseUser: FirebaseUser) {
        val userId = firebaseUser.uid
        val userDocRef = firestore.collection("users").document(userId)

        userDocRef.get().addOnSuccessListener { documentSnapshot ->
            if (documentSnapshot.exists()) {
                saveFcmTokenToFirestore(userId) {
                    val gender = documentSnapshot.getString("gender")
                    if (gender.isNullOrEmpty()) {
                        navigateTo(GenderSelectionActivity::class.java)
                    } else {
                        navigateTo(MainActivity::class.java)
                    }
                }
            } else {
                createNewUserDocument(firebaseUser, firebaseUser.displayName ?: "", firebaseUser.email ?: "")
            }
        }
    }

    private fun saveFcmTokenToFirestore(userId: String, onComplete: () -> Unit) {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val token = task.result
                val tokenData = hashMapOf("fcmToken" to token)
                firestore.collection("users").document(userId)
                    .set(tokenData, SetOptions.merge())
                    .addOnCompleteListener { onComplete() }
            } else {
                // Tetap panggil onComplete meskipun gagal dapat token
                Log.w("FCM", "Fetching FCM registration token failed", task.exception)
                onComplete()
            }
        }
    }

    private fun saveFcmTokenAndNavigate(userId: String, destination: Class<*>) {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val token = task.result
                val tokenData = hashMapOf("fcmToken" to token)
                firestore.collection("users").document(userId)
                    .set(tokenData, SetOptions.merge())
                    .addOnCompleteListener {
                        navigateTo(destination)
                    }
            } else {
                // Tetap lanjut meskipun gagal dapat token
                navigateTo(destination)
            }
        }
    }

    private fun navigateTo(activityClass: Class<*>) {
        val intent = Intent(requireActivity(), activityClass)
        startActivity(intent)
        requireActivity().finishAffinity()
    }
}
