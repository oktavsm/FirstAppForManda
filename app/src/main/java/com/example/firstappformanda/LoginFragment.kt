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

class LoginFragment : Fragment() {

    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var googleSignInClient: GoogleSignInClient

    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnLogin: MaterialButton
    private lateinit var btnGoogleSignIn: SignInButton

    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)!!
                firebaseAuthWithGoogle(account)
            } catch (e: ApiException) {
                Log.w("GoogleSignIn", "Google sign in failed in LoginFragment", e)
                Toast.makeText(requireContext(), "Login Google Gagal", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_login, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        firebaseAuth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        initializeViews(view)
        configureGoogleSignIn()

        btnLogin.setOnClickListener { loginWithEmail() }
        btnGoogleSignIn.setOnClickListener { signInWithGoogle() }
    }

    private fun initializeViews(view: View) {
        etEmail = view.findViewById(R.id.et_login_email)
        etPassword = view.findViewById(R.id.et_login_password)
        btnLogin = view.findViewById(R.id.btn_login)
        btnGoogleSignIn = view.findViewById(R.id.btn_google_sign_in_login)
    }

    private fun configureGoogleSignIn() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(requireActivity(), gso)
    }

    private fun loginWithEmail() {
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString().trim()

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(requireContext(), "Email dan Password harus diisi!", Toast.LENGTH_SHORT).show()
            return
        }

        firebaseAuth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    checkUserProfile(task.result.user!!)
                } else {
                    Toast.makeText(requireContext(), "Login Gagal: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun signInWithGoogle() {
        googleSignInClient.signOut().addOnCompleteListener {
            val signInIntent = googleSignInClient.signInIntent
            googleSignInLauncher.launch(signInIntent)
        }
    }

    private fun firebaseAuthWithGoogle(account: GoogleSignInAccount) {
        val credential = GoogleAuthProvider.getCredential(account.idToken!!, null)
        firebaseAuth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    checkUserProfile(task.result.user!!)
                } else {
                    Toast.makeText(requireContext(), "Autentikasi Firebase Gagal", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun checkUserProfile(firebaseUser: FirebaseUser) {
        val userId = firebaseUser.uid
        val userDocRef = firestore.collection("users").document(userId)

        userDocRef.get().addOnSuccessListener { documentSnapshot ->
            if (documentSnapshot.exists()) {
                // User sudah ada, update FCM token dan navigasi
                saveFcmTokenAndNavigate(userId, documentSnapshot.getString("gender"), false)
            } else {
                // User baru, buat dokumen baru, simpan FCM token, dan navigasi
                val newUser = hashMapOf(
                    "userId" to userId,
                    "nama" to firebaseUser.displayName,
                    "email" to firebaseUser.email,
                    "gender" to "", // Gender akan diisi nanti
                    "coupleId" to "" // Couple ID akan diisi nanti
                )
                userDocRef.set(newUser).addOnSuccessListener {
                    saveFcmTokenAndNavigate(userId, "", true)
                }.addOnFailureListener { e ->
                    Toast.makeText(requireContext(), "Gagal membuat profil baru: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }.addOnFailureListener {
            Toast.makeText(requireContext(), "Gagal memeriksa profil.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveFcmTokenAndNavigate(userId: String, gender: String?, isNewUser: Boolean) {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val token = task.result
                val tokenData = hashMapOf("fcmToken" to token)

                firestore.collection("users").document(userId)
                    .set(tokenData, SetOptions.merge())
                    .addOnSuccessListener {
                        Log.d("LoginFragment", "FCM token saved successfully for user: $userId")
                        if (isNewUser || gender.isNullOrEmpty()) {
                            navigateTo(GenderSelectionActivity::class.java)
                        } else {
                            navigateTo(MainActivity::class.java)
                        }
                    }
                    .addOnFailureListener { e ->
                        Log.w("LoginFragment", "Error saving FCM token for user: $userId", e)
                        // Tetap lanjut meskipun gagal menyimpan token, karena login sudah berhasil
                        if (isNewUser || gender.isNullOrEmpty()) {
                            navigateTo(GenderSelectionActivity::class.java)
                        } else {
                            navigateTo(MainActivity::class.java)
                        }
                    }
            } else {
                Log.w("LoginFragment", "Fetching FCM registration token failed for user: $userId", task.exception)
                // Tetap lanjut meskipun gagal mendapatkan token FCM, karena login sudah berhasil
                if (isNewUser || gender.isNullOrEmpty()) {
                    navigateTo(GenderSelectionActivity::class.java)
                } else {
                    navigateTo(MainActivity::class.java)
                }
            }
        }
    }
    private fun navigateTo(activityClass: Class<*>) {
        val intent = Intent(requireActivity(), activityClass)
        startActivity(intent)
        requireActivity().finishAffinity()
    }
}
