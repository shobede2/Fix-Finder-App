package com.example.fix_finder

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.fix_finder.data.local.SessionManager
import com.google.android.material.bottomnavigation.BottomNavigationView

class HomeFragment : Fragment() {

    private lateinit var sessionManager: SessionManager

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sessionManager = SessionManager.getInstance(requireContext())
        val user = sessionManager.getUser()

        val tvUserGreeting = view.findViewById<TextView>(R.id.tvUserGreeting)
        val tvUserLocation = view.findViewById<TextView>(R.id.tvUserLocation)
        val btnNotification = view.findViewById<ImageView>(R.id.btnNotification)

        if (user != null) {
            val firstName = user.name.trim().substringBefore(" ").replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
            tvUserGreeting?.text = "Hello, $firstName! 👋"
            tvUserLocation?.text = "Your location: ${user.location}"
        }

        btnNotification?.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, NotificationsFragment())
                .addToBackStack(null)
                .commit()
        }

        // Location change click
        view.findViewById<View>(R.id.textChange)?.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, SettingsFragment())
                .addToBackStack(null)
                .commit()
        }

        // Category Cards Navigation
        view.findViewById<View>(R.id.cardSearchBar)?.setOnClickListener { navigateToSearch(null) }
        view.findViewById<View>(R.id.btnFindTechnician)?.setOnClickListener { navigateToSearch(null) }

        view.findViewById<View>(R.id.cardPlumbing)?.setOnClickListener { navigateToSearch("plumbing") }
        view.findViewById<View>(R.id.cardElectrical)?.setOnClickListener { navigateToSearch("electrical") }
        view.findViewById<View>(R.id.cardAppliances)?.setOnClickListener { navigateToSearch("appliances") }
        view.findViewById<View>(R.id.cardAC)?.setOnClickListener { navigateToSearch("ac") }
        view.findViewById<View>(R.id.cardElectronics)?.setOnClickListener { navigateToSearch("electronics") }
        view.findViewById<View>(R.id.cardAutomotive)?.setOnClickListener { navigateToSearch("automotive") }
        view.findViewById<View>(R.id.cardHandyman)?.setOnClickListener { navigateToSearch("handyman") }
        view.findViewById<View>(R.id.cardOther)?.setOnClickListener { navigateToSearch("other") }

        // Bottom Navigation Bar
        val bottomNav = view.findViewById<BottomNavigationView>(R.id.bottomNavigation)
        bottomNav?.selectedItemId = R.id.nav_home
        bottomNav?.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> true
                R.id.nav_search -> {
                    navigateToSearch(null)
                    true
                }
                R.id.nav_bookings -> {
                    parentFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, BookingsFragment())
                        .addToBackStack(null)
                        .commit()
                    true
                }
                R.id.nav_messages -> {
                    parentFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, MessagesFragment())
                        .addToBackStack(null)
                        .commit()
                    true
                }
                R.id.nav_profile -> {
                    parentFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, SettingsFragment())
                        .addToBackStack(null)
                        .commit()
                    true
                }
                else -> false
            }
        }
    }

    private fun navigateToSearch(category: String?) {
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, SearchFragment.newInstance(category))
            .addToBackStack(null)
            .commit()
    }
}
