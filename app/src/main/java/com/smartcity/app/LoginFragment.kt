package com.smartcity.app

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.smartcity.app.model.User


// MainActivity'i Fragment'lara böldüğümüz için buraya taşıdık, giriş ekranı
class LoginFragment : Fragment(R.layout.fragment_login) {

    private lateinit var auth: FirebaseAuth

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = Firebase.auth

        val etEmail = view.findViewById<TextInputEditText>(R.id.etEmail)
        val etPassword = view.findViewById<TextInputEditText>(R.id.etPassword)
        val emailInputLayout = view.findViewById<TextInputLayout>(R.id.emailInputLayout)
        val passwordInputLayout = view.findViewById<TextInputLayout>(R.id.passwordInputLayout)
        val btnLogin = view.findViewById<MaterialButton>(R.id.btnLogin)
        val btnSignUp = view.findViewById<MaterialButton>(R.id.btnSignUp)
        val btnGuestLogin = view.findViewById<MaterialButton>(R.id.btnGuestLogin)

        btnLogin.setOnClickListener {
            val emailText = etEmail.text.toString().trim()
            val passwordText = etPassword.text.toString().trim()

            if (emailText.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(emailText).matches()) {
                emailInputLayout.error = "Enter a valid email."
                return@setOnClickListener
            } else emailInputLayout.error = null

            if (passwordText.isEmpty() || passwordText.length < 6) {
                passwordInputLayout.error = "Password must be at least 6 characters"
                return@setOnClickListener
            } else passwordInputLayout.error = null

            loginWithEmail(emailText, passwordText,
                onSuccess = { user ->
                    showMessage("Successful")
                            updateUI(user)
                },
                onFailure = { exception ->
                    showMessage("Error: ${exception?.localizedMessage}")
                    updateUI(null)
                }
            )
        }

        btnSignUp.setOnClickListener {
            val emailText = etEmail.text.toString().trim()
            val passwordText = etPassword.text.toString().trim()

            if (emailText.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(emailText).matches()) {
                emailInputLayout.error = "Enter a valid email."
                return@setOnClickListener
            } else emailInputLayout.error = null

            if (passwordText.isEmpty() || passwordText.length < 6) {
                passwordInputLayout.error = "Password must be at least 6 characters"
                return@setOnClickListener
            } else passwordInputLayout.error = null

            registerWithEmail(emailText, passwordText,
                onSuccess = { user ->
                    showMessage("Registration Successful")
                    updateUI(user)
                },
                onFailure = { exception ->
                    showMessage("Registration Error: ${exception?.localizedMessage}")
                    updateUI(null)
                }
            )
        }

        btnGuestLogin.setOnClickListener {
            signInAsGuest()
        }
    }

    override fun onStart() {
        super.onStart()
        val currentUser = auth.currentUser
        if (currentUser != null) {
            updateUI(currentUser)
        }
    }

    private fun showMessage(message: String) {
        // Toast'ta MainActivity'de yaptığımız this yerine Fragment'ta requireContext() kullanılır
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
    }

    private fun signInAsGuest() {
        auth.signInAnonymously()
            .addOnCompleteListener(requireActivity()) { task ->
                if (task.isSuccessful) {
                    updateUI(auth.currentUser)
                } else {
                    Log.w("LoginFragment", "signInAnonymously:failure", task.exception)
                    updateUI(null)
                }
            }
    }

    private fun loginWithEmail(email: String, password: String, onSuccess: (FirebaseUser?) -> Unit, onFailure: (Exception?) -> Unit) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) onSuccess(auth.currentUser) else onFailure(task.exception)
            }
    }

    private fun registerWithEmail(email: String, password: String, onSuccess: (FirebaseUser?) -> Unit, onFailure: (Exception?) -> Unit) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val firebaseUser = auth.currentUser
                    if (firebaseUser != null) {

                        val db = FirebaseFirestore.getInstance()

                        val newUser = User(
                            uid = firebaseUser.uid,
                            email = email,
                            role = "citizen", // Varsayılan
                            points = 0
                        )

                        //Firestore'da user klasörü açıp içine kullanıcıları yazar
                        db.collection("users").document(firebaseUser.uid).set(newUser)
                            .addOnSuccessListener {
                                onSuccess(firebaseUser)
                            }
                            .addOnFailureListener { e ->
                                onFailure(e)
                            }
                    }
                } else {
                    onFailure(task.exception)
                }
            }
    }

    //Rol fonksiyonu güncellendi : uygulamaya kayıt olan her kullanıcı default olarak citizen atanır.
    //Firestore üzerinden kayıtlı kullanıcıların rolleri değiştirilebilir (örn : citizen -> admin olabilir)
    private fun updateUI(user: FirebaseUser?) {
        if (user != null) {
            val db = FirebaseFirestore.getInstance()
            db.collection("users").document(user.uid).get()
                .addOnSuccessListener { document ->
                    if (!isAdded) return@addOnSuccessListener
                    val role = document?.getString("role")?.lowercase() ?: "citizen"
                    if (role == "admin") {
                        findNavController().navigate(R.id.action_loginFragment_to_adminFragment)
                    } else {
                        findNavController().navigate(R.id.action_loginFragment_to_homeFragment)
                    }
                }
                .addOnFailureListener {
                    if (!isAdded) return@addOnFailureListener
                    findNavController().navigate(R.id.action_loginFragment_to_homeFragment)
                }
        }
    }
}
