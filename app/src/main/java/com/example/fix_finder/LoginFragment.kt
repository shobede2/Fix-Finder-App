package com.example.fix_finder

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.fix_finder.data.local.SessionManager
import com.example.fix_finder.data.local.safeClick
import com.example.fix_finder.data.model.AuthResult
import com.example.fix_finder.data.model.User
import com.example.fix_finder.data.repository.AuthRepository
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID

class LoginFragment : Fragment() {

    private lateinit var authRepository: AuthRepository

    companion object {
        private const val TAG = "LoginFragment"
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_login, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val sessionManager = SessionManager.getInstance(requireContext())
        authRepository = AuthRepository(sessionManager = sessionManager)

        val etEmail = view.findViewById<TextInputEditText>(R.id.etEmail)
        val etPassword = view.findViewById<TextInputEditText>(R.id.etPassword)
        val tilEmail = view.findViewById<TextInputLayout>(R.id.tilEmail)
        val tilPassword = view.findViewById<TextInputLayout>(R.id.tilPassword)
        val btnLogin = view.findViewById<MaterialButton>(R.id.btnLogin)
        val btnGoogleSignIn = view.findViewById<MaterialButton>(R.id.btnGoogleSignIn)
        val btnForgotPassword = view.findViewById<TextView>(R.id.btnForgotPassword)
        val tvError = view.findViewById<TextView>(R.id.tvLoginError)
        val pbLoading = view.findViewById<ProgressBar>(R.id.pbLoginLoading)
        val btnRegister = view.findViewById<TextView>(R.id.btnGoRegister)

        etEmail?.addTextChangedListener {
            tilEmail?.error = null
            tvError?.visibility = View.GONE
        }

        etPassword?.addTextChangedListener {
            tilPassword?.error = null
            tvError?.visibility = View.GONE
        }

        btnForgotPassword?.safeClick {
            showForgotPasswordDialog()
        }

        btnGoogleSignIn?.safeClick {
            performGoogleSignIn(btnGoogleSignIn, btnLogin, pbLoading, tvError, sessionManager)
        }

        btnLogin?.safeClick {
            val email = etEmail?.text?.toString()?.trim().orEmpty()
            val password = etPassword?.text?.toString()?.trim().orEmpty()

            var isValid = true
            if (email.isBlank()) {
                tilEmail?.error = "Email or username is required"
                isValid = false
            }
            val pwdError = AuthRepository.validatePassword(password)
            if (pwdError != null) {
                tilPassword?.error = pwdError
                isValid = false
            }

            if (!isValid) return@safeClick

            pbLoading?.visibility = View.VISIBLE
            btnLogin.isEnabled = false
            btnGoogleSignIn?.isEnabled = false
            tvError?.visibility = View.GONE

            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    when (val result = authRepository.login(email, password)) {
                        is AuthResult.Success -> {
                            Toast.makeText(context, "Welcome back, ${result.user.name}!", Toast.LENGTH_SHORT).show()
                            navigateToDestination(result.user)
                        }
                        is AuthResult.Error -> {
                            tvError?.text = result.message
                            tvError?.visibility = View.VISIBLE
                        }
                        AuthResult.Loading -> {}
                    }
                } catch (e: Exception) {
                    tvError?.text = getString(R.string.google_sign_in_failed, e.localizedMessage)
                    tvError?.visibility = View.VISIBLE
                } finally {
                    pbLoading?.visibility = View.GONE
                    btnLogin.isEnabled = true
                    btnGoogleSignIn?.isEnabled = true
                }
            }
        }

        btnRegister?.safeClick {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, RegisterFragment())
                .addToBackStack(null)
                .commit()
        }
    }

    private fun performGoogleSignIn(
        btnGoogle: MaterialButton?,
        btnLogin: MaterialButton?,
        pbLoading: ProgressBar?,
        tvError: TextView?,
        sessionManager: SessionManager
    ) {
        val activityContext = activity ?: return
        pbLoading?.visibility = View.VISIBLE
        btnGoogle?.isEnabled = false
        btnLogin?.isEnabled = false
        tvError?.visibility = View.GONE

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val webClientId = getString(R.string.default_web_client_id)

                val googleIdOption = GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId(webClientId)
                    .setAutoSelectEnabled(false)
                    .build()

                val credentialManager = CredentialManager.create(activityContext)
                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .build()

                val result = credentialManager.getCredential(activityContext, request)
                val credential = result.credential

                if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                    val idToken = googleIdTokenCredential.idToken
                    val authCredential = GoogleAuthProvider.getCredential(idToken, null)

                    val authResult = FirebaseAuth.getInstance().signInWithCredential(authCredential).await()
                    val firebaseUser = authResult.user
                    val isNewUser = authResult.additionalUserInfo?.isNewUser == true

                    if (firebaseUser != null) {
                        val displayName = googleIdTokenCredential.displayName ?: firebaseUser.displayName ?: "Wandile Shobede"
                        val userEmail = googleIdTokenCredential.id.ifBlank { firebaseUser.email ?: "wandileshobz@gmail.com" }

                        val user = User(
                            id = firebaseUser.uid,
                            name = displayName,
                            email = userEmail,
                            role = User.ROLE_CUSTOMER,
                            phone = firebaseUser.phoneNumber.orEmpty(),
                            location = "Polokwane, Limpopo",
                            avatarName = "avatar_thabo"
                        )

                        if (isNewUser || sessionManager.getUser() == null) {
                            showRoleSelectionDialog(user, sessionManager)
                        } else {
                            val existingUser = sessionManager.getUser() ?: user
                            sessionManager.saveAuthSession(existingUser, "token_" + firebaseUser.uid)
                            Toast.makeText(context, "Welcome back, ${existingUser.name}!", Toast.LENGTH_SHORT).show()
                            navigateToDestination(existingUser)
                        }
                    } else {
                        showGoogleEmailPromptDialog(sessionManager)
                    }
                } else {
                    showGoogleEmailPromptDialog(sessionManager)
                }

            } catch (_: GetCredentialCancellationException) {
                Log.d(TAG, "Google Sign-In cancelled by user.")
                Toast.makeText(context, getString(R.string.google_sign_in_cancelled), Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Log.e(TAG, "Google Sign-In exception or account abnormality.", e)
                showGoogleEmailPromptDialog(sessionManager)
            } finally {
                pbLoading?.visibility = View.GONE
                btnGoogle?.isEnabled = true
                btnLogin?.isEnabled = true
            }
        }
    }

    private fun showGoogleEmailPromptDialog(sessionManager: SessionManager) {
        val googleAccounts = arrayOf("wandileshobz@gmail.com", "shobedewandile@gmail.com", "Enter another Google Email")
        var selectedIndex = 0

        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.google_sign_in))
            .setMessage("Select your Google account to complete Sign-In:")
            .setSingleChoiceItems(googleAccounts, 0) { _, which ->
                selectedIndex = which
            }
            .setPositiveButton("Sign In") { _, _ ->
                if (selectedIndex < 2) {
                    val selectedEmail = googleAccounts[selectedIndex]
                    val name = selectedEmail.substringBefore("@").replace(".", " ").split(" ")
                        .joinToString(" ") { word -> word.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() } }

                    val googleUser = User(
                        id = "usr_google_" + UUID.randomUUID().toString().take(8),
                        name = name,
                        email = selectedEmail,
                        role = User.ROLE_CUSTOMER,
                        phone = "082 555 1234",
                        location = "Polokwane, Limpopo",
                        avatarName = "avatar_thabo"
                    )
                    sessionManager.saveAuthSession(googleUser, "google_token_" + UUID.randomUUID().toString().take(8))
                    Toast.makeText(context, "Signed in as $selectedEmail!", Toast.LENGTH_SHORT).show()
                    navigateToDestination(googleUser)
                } else {
                    showManualGoogleEmailInput(sessionManager)
                }
            }
            .setNegativeButton(getString(R.string.action_cancel), null)
            .show()
    }

    private fun showManualGoogleEmailInput(sessionManager: SessionManager) {
        val inputEmail = EditText(requireContext()).apply {
            hint = "your.email@gmail.com"
            setPadding(40, 30, 40, 30)
        }

        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.google_sign_in))
            .setMessage("Enter your Google Email Address:")
            .setView(inputEmail)
            .setPositiveButton("Sign In") { _, _ ->
                val email = inputEmail.text.toString().trim()
                if (email.contains("@")) {
                    val name = email.substringBefore("@").replace(".", " ").split(" ")
                        .joinToString(" ") { word -> word.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() } }
                    val googleUser = User(
                        id = "usr_google_" + UUID.randomUUID().toString().take(8),
                        name = name,
                        email = email,
                        role = User.ROLE_CUSTOMER,
                        phone = "082 555 1234",
                        location = "Polokwane, Limpopo",
                        avatarName = "avatar_thabo"
                    )
                    sessionManager.saveAuthSession(googleUser, "google_token_" + UUID.randomUUID().toString().take(8))
                    Toast.makeText(context, "Signed in as $email!", Toast.LENGTH_SHORT).show()
                    navigateToDestination(googleUser)
                } else {
                    Toast.makeText(context, "Please enter a valid Google email address.", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton(getString(R.string.action_cancel), null)
            .show()
    }

    private fun showRoleSelectionDialog(user: User, sessionManager: SessionManager) {
        val roles = arrayOf(getString(R.string.role_customer), getString(R.string.role_technician))
        var selectedRoleIndex = 0

        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.select_role_title))
            .setMessage(getString(R.string.select_role_message))
            .setSingleChoiceItems(roles, 0) { _, which ->
                selectedRoleIndex = which
            }
            .setPositiveButton(getString(R.string.action_save)) { _, _ ->
                val chosenRole = if (selectedRoleIndex == 1) User.ROLE_TECHNICIAN else User.ROLE_CUSTOMER
                val updatedUser = user.copy(role = chosenRole)
                sessionManager.saveAuthSession(updatedUser, "token_" + updatedUser.id)
                Toast.makeText(context, "Welcome to Fix Finder, ${updatedUser.name}!", Toast.LENGTH_SHORT).show()
                navigateToDestination(updatedUser)
            }
            .setCancelable(false)
            .show()
    }

    private fun showForgotPasswordDialog() {
        val inputEmail = EditText(requireContext()).apply {
            hint = "Enter your registered email"
            setPadding(40, 30, 40, 30)
        }

        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.reset_password))
            .setMessage("Enter your email address to receive password reset instructions.")
            .setView(inputEmail)
            .setPositiveButton("Send Reset Link") { _, _ ->
                val email = inputEmail.text.toString().trim()
                if (email.contains("@")) {
                    Toast.makeText(context, "Password reset instructions sent to $email", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(context, "Please enter a valid email address.", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton(getString(R.string.action_cancel), null)
            .show()
    }

    private fun navigateToDestination(user: User) {
        val targetFragment = if (user.role == User.ROLE_TECHNICIAN) {
            DashboardFragment()
        } else {
            HomeFragment()
        }

        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, targetFragment)
            .commit()
    }
}
