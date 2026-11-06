package com.example.fix_finder

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ProgressBar
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.fix_finder.data.api.ApiClient
import com.example.fix_finder.data.local.SessionManager
import com.example.fix_finder.data.repository.ResultState
import com.example.fix_finder.data.repository.ServiceRepository
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.launch

class BookingsFragment : Fragment() {

    private lateinit var serviceRepository: ServiceRepository
    private lateinit var sessionManager: SessionManager
    private lateinit var bookingsAdapter: BookingsAdapter

    companion object {
        private const val TAG = "BookingsFragment"
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_bookings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val safeContext = context?.applicationContext ?: return
        serviceRepository = ServiceRepository(ApiClient.apiService, safeContext)
        sessionManager = SessionManager.getInstance(safeContext)

        val pbBookings = view.findViewById<ProgressBar>(R.id.pbBookings)
        val layoutEmpty = view.findViewById<LinearLayout>(R.id.layoutEmptyBookings)
        val rvBookings = view.findViewById<RecyclerView>(R.id.rvBookings)

        bookingsAdapter = BookingsAdapter(emptyList())
        rvBookings?.layoutManager = LinearLayoutManager(requireContext())
        rvBookings?.adapter = bookingsAdapter

        // Setup bottom navigation
        val bottomNav = view.findViewById<BottomNavigationView>(R.id.bottomNavigation)
        bottomNav?.selectedItemId = R.id.nav_bookings
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
                R.id.nav_bookings -> true
                R.id.nav_messages -> {
                    parentFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, MessagesFragment())
                        .commit()
                    true
                }
                R.id.nav_profile -> {
                    parentFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, SettingsFragment())
                        .commit()
                    true
                }
                else -> false
            }
        }

        // Fetch User Requests
        val user = sessionManager.getUser()
        val userId = user?.id ?: "usr_demo"

        pbBookings?.visibility = View.VISIBLE
        layoutEmpty?.visibility = View.GONE

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                // Background sync check for offline created requests
                try {
                    serviceRepository.syncPendingOfflineReports()
                } catch (e: Exception) {
                    Log.e(TAG, "Offline sync check failed", e)
                }

                when (val result = serviceRepository.getUserRequests(userId)) {
                    is ResultState.Success -> {
                        if (!isAdded) return@launch
                        pbBookings?.visibility = View.GONE
                        val requests = result.data
                        if (requests.isEmpty()) {
                            layoutEmpty?.visibility = View.VISIBLE
                            rvBookings?.visibility = View.GONE
                        } else {
                            layoutEmpty?.visibility = View.GONE
                            rvBookings?.visibility = View.VISIBLE
                            bookingsAdapter.updateData(requests)
                        }
                    }
                    is ResultState.Error -> {
                        if (!isAdded) return@launch
                        pbBookings?.visibility = View.GONE
                        layoutEmpty?.visibility = View.VISIBLE
                        rvBookings?.visibility = View.GONE
                    }
                    ResultState.Loading -> {}
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load bookings", e)
                if (isAdded) {
                    pbBookings?.visibility = View.GONE
                    layoutEmpty?.visibility = View.VISIBLE
                    rvBookings?.visibility = View.GONE
                }
            }
        }
    }
}
