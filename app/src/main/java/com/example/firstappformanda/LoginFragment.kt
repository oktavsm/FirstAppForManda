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

class LoginFragment : Fragment() {

    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var googleSignInClient: GoogleSignInClient

    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnLogin: MaterialButton
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
                Log.w("GoogleSignIn", "Google sign in failed", e)
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
                    // Jika login berhasil, cek profilnya
                    checkUserProfile(task.result.user!!)
                } else {
                    Toast.makeText(requireContext(), "Login Gagal: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun signInWithGoogle() {
        val signInIntent = googleSignInClient.signInIntent
        googleSignInLauncher.launch(signInIntent)
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

    // Ini fungsi "penentu arah" yang sama persis seperti di RegisterFragment
    private fun checkUserProfile(firebaseUser: FirebaseUser) {
        val userId = firebaseUser.uid
        val userDocRef = firestore.collection("users").document(userId)

        userDocRef.get().addOnSuccessListener { documentSnapshot ->
            if (documentSnapshot.exists()) {
                val gender = documentSnapshot.getString("gender")
                if (gender.isNullOrEmpty()) {
                    // Jika profil ada tapi gender kosong -> ke halaman pilih gender
                    navigateTo(GenderSelectionActivity::class.java)
                } else {
                    // Jika profil lengkap -> ke halaman utama
                    navigateTo(MainActivity::class.java)
                }
            } else {
                // Jika user login Google pertama kali, buat profil baru
                val newUser = hashMapOf(
                    "userId" to userId, "nama" to firebaseUser.displayName,
                    "email" to firebaseUser.email, "gender" to "", "coupleId" to ""
                )
                userDocRef.set(newUser).addOnSuccessListener {
                    navigateTo(GenderSelectionActivity::class.java)
                }
            }
        }.addOnFailureListener {
            Toast.makeText(requireContext(), "Gagal memeriksa profil.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun navigateTo(activityClass: Class<*>) {
        val intent = Intent(requireActivity(), activityClass)
        startActivity(intent)
        requireActivity().finishAffinity()
    }
}
