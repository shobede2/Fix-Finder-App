package com.example.fix_finder

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SwitchCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.lifecycleScope
import com.example.fix_finder.data.local.LocaleHelper
import com.example.fix_finder.data.local.SessionManager
import com.example.fix_finder.data.model.User
import com.example.fix_finder.data.model.UserSettings
import com.example.fix_finder.data.repository.SettingsRepository
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch

class SettingsFragment : Fragment() {

    private lateinit var sessionManager: SessionManager
    private lateinit var settingsRepository: SettingsRepository

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sessionManager = SessionManager.getInstance(requireContext())
        settingsRepository = SettingsRepository(sessionManager = sessionManager)

        val btnBack = view.findViewById<ImageView>(R.id.btnBack)
        val imgSettingsAvatar = view.findViewById<ImageView>(R.id.imgSettingsAvatar)
        val tvProfileName = view.findViewById<TextView>(R.id.tvProfileName)
        val tvProfileEmail = view.findViewById<TextView>(R.id.tvProfileEmail)
        val tvProfileRole = view.findViewById<TextView>(R.id.tvProfileRole)
        val btnEditProfile = view.findViewById<MaterialButton>(R.id.btnEditProfile)

        val switchPush = view.findViewById<SwitchCompat>(R.id.switchPush)
        val switchEmail = view.findViewById<SwitchCompat>(R.id.switchEmail)
        val switchSms = view.findViewById<SwitchCompat>(R.id.switchSms)

        val spLocation = view.findViewById<Spinner>(R.id.spLocation)

        val rgLanguage = view.findViewById<RadioGroup>(R.id.rgLanguage)
        val rbLangEnglish = view.findViewById<RadioButton>(R.id.rbLangEnglish)
        val rbLangIsizulu = view.findViewById<RadioButton>(R.id.rbLangIsizulu)

        val rgTheme = view.findViewById<RadioGroup>(R.id.rgTheme)
        val rbThemeSystem = view.findViewById<RadioButton>(R.id.rbThemeSystem)
        val rbThemeLight = view.findViewById<RadioButton>(R.id.rbThemeLight)
        val rbThemeDark = view.findViewById<RadioButton>(R.id.rbThemeDark)

        val btnLogout = view.findViewById<MaterialButton>(R.id.btnLogout)

        btnBack?.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        // Display current user profile info
        fun bindUserProfile() {
            val user = sessionManager.getUser()
            if (user != null) {
                tvProfileName?.text = user.name
                tvProfileEmail?.text = user.email
                tvProfileRole?.text = "Role: ${user.role.replaceFirstChar { it.uppercase() }}"
                val avatarRes = getAvatarDrawableRes(user.avatarName)
                imgSettingsAvatar?.setImageResource(avatarRes)
            } else {
                tvProfileName?.text = "Guest User"
                tvProfileEmail?.text = "Not logged in"
                tvProfileRole?.text = "Role: Guest"
            }
        }

        bindUserProfile()

        // Edit Profile Button Click
        btnEditProfile?.setOnClickListener {
            showEditProfileDialog {
                bindUserProfile()
            }
        }

        val user = sessionManager.getUser()

        // Location Spinner adapter
        val locations = listOf("Polokwane, Limpopo", "Johannesburg, Gauteng", "Pretoria, Gauteng", "Cape Town, Western Cape", "Durban, KwaZulu-Natal")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, locations)
        spLocation?.adapter = adapter

        // Load current settings
        val currentSettings = settingsRepository.getSettings()
        switchPush?.isChecked = currentSettings.pushNotifications
        switchEmail?.isChecked = currentSettings.emailAlerts
        switchSms?.isChecked = currentSettings.smsAlerts

        val locationIndex = locations.indexOf(currentSettings.preferredLocation).coerceAtLeast(0)
        spLocation?.setSelection(locationIndex)

        if (currentSettings.preferredLanguage == LocaleHelper.LANG_ISIZULU) {
            rbLangIsizulu?.isChecked = true
        } else {
            rbLangEnglish?.isChecked = true
        }

        when (currentSettings.themeMode) {
            UserSettings.THEME_LIGHT -> rbThemeLight?.isChecked = true
            UserSettings.THEME_DARK -> rbThemeDark?.isChecked = true
            else -> rbThemeSystem?.isChecked = true
        }

        // Save settings whenever changed
        val saveAction = {
            val themeMode = when (rgTheme?.checkedRadioButtonId) {
                R.id.rbThemeLight -> UserSettings.THEME_LIGHT
                R.id.rbThemeDark -> UserSettings.THEME_DARK
                else -> UserSettings.THEME_SYSTEM
            }

            val newLang = if (rgLanguage?.checkedRadioButtonId == R.id.rbLangIsizulu) {
                LocaleHelper.LANG_ISIZULU
            } else {
                LocaleHelper.LANG_ENGLISH
            }

            MainActivity.applyTheme(themeMode)

            val newSettings = UserSettings(
                userId = user?.id ?: "guest",
                pushNotifications = switchPush?.isChecked ?: true,
                emailAlerts = switchEmail?.isChecked ?: true,
                smsAlerts = switchSms?.isChecked ?: false,
                preferredLocation = spLocation?.selectedItem?.toString() ?: locations[0],
                themeMode = themeMode,
                preferredLanguage = newLang
            )

            if (currentSettings.preferredLanguage != newLang) {
                LocaleHelper.setLocale(requireContext(), newLang)
            }

            lifecycleScope.launch {
                settingsRepository.saveSettings(newSettings)
            }
        }

        switchPush?.setOnCheckedChangeListener { _, _ -> saveAction() }
        switchEmail?.setOnCheckedChangeListener { _, _ -> saveAction() }
        switchSms?.setOnCheckedChangeListener { _, _ -> saveAction() }
        rgLanguage?.setOnCheckedChangeListener { _, _ ->
            saveAction()
            activity?.recreate()
        }
        rgTheme?.setOnCheckedChangeListener { _, _ -> saveAction() }

        // Logout handling
        btnLogout?.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Log Out")
                .setMessage("Are you sure you want to log out of Fix Finder?")
                .setPositiveButton("Log Out") { _, _ ->
                    sessionManager.logout()
                    Toast.makeText(context, "Logged out successfully.", Toast.LENGTH_SHORT).show()

                    // Clear fragment back stack and return to LoginFragment
                    parentFragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
                    parentFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, LoginFragment())
                        .commit()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        // Bottom Navigation Bar
        val bottomNav = view.findViewById<BottomNavigationView>(R.id.bottomNavigation)
        if (user?.role == User.ROLE_TECHNICIAN) {
            bottomNav?.menu?.clear()
            bottomNav?.inflateMenu(R.menu.technician_bottom_menu)
            bottomNav?.selectedItemId = R.id.nav_tech_profile
            bottomNav?.setOnItemSelectedListener { item ->
                when (item.itemId) {
                    R.id.nav_dashboard -> {
                        parentFragmentManager.beginTransaction()
                            .replace(R.id.fragment_container, DashboardFragment())
                            .commit()
                        true
                    }
                    R.id.nav_jobs, R.id.nav_earnings -> {
                        parentFragmentManager.beginTransaction()
                            .replace(R.id.fragment_container, BookingsFragment())
                            .commit()
                        true
                    }
                    R.id.nav_tech_profile -> true
                    else -> false
                }
            }
        } else {
            bottomNav?.selectedItemId = R.id.nav_profile
            bottomNav?.setOnItemSelectedListener { item ->
                when (item.itemId) {
                    R.id.nav_home -> {
                        parentFragmentManager.beginTransaction()
                            .replace(R.id.fragment_container, HomeFragment())
                            .commit()
                        true
                    }
                    R.id.nav_search -> {
                        parentFragmentManager.beginTransaction()
                            .replace(R.id.fragment_container, SearchFragment())
                            .commit()
                        true
                    }
                    R.id.nav_bookings -> {
                        parentFragmentManager.beginTransaction()
                            .replace(R.id.fragment_container, BookingsFragment())
                            .commit()
                        true
                    }
                    R.id.nav_messages -> {
                        parentFragmentManager.beginTransaction()
                            .replace(R.id.fragment_container, MessagesFragment())
                            .commit()
                        true
                    }
                    R.id.nav_profile -> true
                    else -> false
                }
            }
        }
    }

    private fun showEditProfileDialog(onSaved: () -> Unit) {
        val user = sessionManager.getUser() ?: User("usr_demo", "Demo User", "demo@example.com")
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_edit_profile, null)

        val etEditName = dialogView.findViewById<TextInputEditText>(R.id.etEditName)
        val etEditPhone = dialogView.findViewById<TextInputEditText>(R.id.etEditPhone)
        val etEditLocation = dialogView.findViewById<TextInputEditText>(R.id.etEditLocation)

        val containerThabo = dialogView.findViewById<FrameLayout>(R.id.containerAvatarThabo)
        val containerSipho = dialogView.findViewById<FrameLayout>(R.id.containerAvatarSipho)
        val containerLerato = dialogView.findViewById<FrameLayout>(R.id.containerAvatarLerato)

        val btnCancel = dialogView.findViewById<MaterialButton>(R.id.btnCancelEdit)
        val btnSave = dialogView.findViewById<MaterialButton>(R.id.btnSaveEdit)

        etEditName?.setText(user.name)
        etEditPhone?.setText(user.phone)
        etEditLocation?.setText(user.location)

        var selectedAvatarName = user.avatarName.ifBlank { "avatar_thabo" }

        fun updateAvatarSelectionUI() {
            val navyColor = requireContext().getColor(R.color.primary_navy)
            val lightBorder = requireContext().getColor(R.color.border_light)

            containerThabo?.backgroundTintList = ColorStateList.valueOf(
                if (selectedAvatarName == "avatar_thabo") navyColor else lightBorder
            )
            containerSipho?.backgroundTintList = ColorStateList.valueOf(
                if (selectedAvatarName == "avatar_sipho") navyColor else lightBorder
            )
            containerLerato?.backgroundTintList = ColorStateList.valueOf(
                if (selectedAvatarName == "avatar_lerato") navyColor else lightBorder
            )
        }

        updateAvatarSelectionUI()

        containerThabo?.setOnClickListener { selectedAvatarName = "avatar_thabo"; updateAvatarSelectionUI() }
        containerSipho?.setOnClickListener { selectedAvatarName = "avatar_sipho"; updateAvatarSelectionUI() }
        containerLerato?.setOnClickListener { selectedAvatarName = "avatar_lerato"; updateAvatarSelectionUI() }

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setCancelable(true)
            .create()

        btnCancel?.setOnClickListener { dialog.dismiss() }

        btnSave?.setOnClickListener {
            val newName = etEditName?.text?.toString()?.trim().orEmpty()
            val newPhone = etEditPhone?.text?.toString()?.trim().orEmpty()
            val newLocation = etEditLocation?.text?.toString()?.trim().orEmpty()

            if (newName.isBlank()) {
                etEditName?.error = "Name cannot be empty"
                return@setOnClickListener
            }

            val updatedUser = user.copy(
                name = newName,
                phone = newPhone,
                location = newLocation.ifBlank { "Polokwane, Limpopo" },
                avatarName = selectedAvatarName
            )

            sessionManager.updateUser(updatedUser)
            Toast.makeText(context, "Profile updated successfully!", Toast.LENGTH_SHORT).show()
            onSaved()
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun getAvatarDrawableRes(name: String): Int {
        return when (name) {
            "avatar_thabo" -> R.drawable.avatar_thabo
            "avatar_sipho" -> R.drawable.avatar_sipho
            "avatar_lerato" -> R.drawable.avatar_lerato
            else -> R.drawable.avatar_thabo
        }
    }
}
