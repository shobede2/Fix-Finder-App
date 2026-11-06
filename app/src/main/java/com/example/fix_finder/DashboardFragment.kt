package com.example.fix_finder

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.example.fix_finder.data.local.SessionManager
import com.google.android.material.bottomnavigation.BottomNavigationView

class DashboardFragment : Fragment() {

    private lateinit var sessionManager: SessionManager

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_dashboard, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sessionManager = SessionManager.getInstance(requireContext())
        val user = sessionManager.getUser()

        val tvTechGreeting = view.findViewById<TextView>(R.id.tvTechGreeting)
        val btnNotification = view.findViewById<ImageView>(R.id.btnNotification)

        val cardJobs = view.findViewById<View>(R.id.cardJobs)
        val cardPending = view.findViewById<View>(R.id.cardPending)
        val cardEarnings = view.findViewById<View>(R.id.cardEarnings)

        if (user != null) {
            val name = user.name.substringBefore(" ")
            tvTechGreeting?.text = "Good morning, $name 👋"
        }

        btnNotification?.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, NotificationsFragment())
                .addToBackStack(null)
                .commit()
        }

        cardJobs?.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, BookingsFragment())
                .addToBackStack(null)
                .commit()
        }

        cardPending?.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, BookingsFragment())
                .addToBackStack(null)
                .commit()
        }

        cardEarnings?.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Monthly Earnings Summary")
                .setMessage("• Completed Jobs: 32 jobs\n• Gross Revenue: R18,450.00\n• Service Fee (5%): -R922.50\n• Net Earnings: R17,527.50\n\nNext automated payout: Friday, May 16.")
                .setPositiveButton("OK", null)
                .show()
        }

        val bottomNav = view.findViewById<BottomNavigationView>(R.id.bottomNavigation)
        bottomNav?.selectedItemId = R.id.nav_dashboard
        bottomNav?.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_dashboard -> true
                R.id.nav_jobs, R.id.nav_earnings -> {
                    parentFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, BookingsFragment())
                        .addToBackStack(null)
                        .commit()
                    true
                }
                R.id.nav_tech_profile -> {
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
}
