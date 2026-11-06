package com.example.fix_finder

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.fix_finder.data.local.SessionManager
import com.example.fix_finder.data.model.User
import com.example.fix_finder.data.repository.ChatRepository
import com.google.android.material.bottomnavigation.BottomNavigationView

class MessagesFragment : Fragment() {

    private lateinit var messagesAdapter: MessagesAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_messages, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val sessionManager = SessionManager.getInstance(requireContext())
        val user = sessionManager.getUser()

        val btnBack = view.findViewById<ImageView>(R.id.btnBack)
        val btnNotification = view.findViewById<ImageView>(R.id.btnNotification)
        val rvConversations = view.findViewById<RecyclerView>(R.id.rvConversations)
        val layoutEmpty = view.findViewById<LinearLayout>(R.id.layoutMessagesEmpty)

        btnBack?.setOnClickListener { parentFragmentManager.popBackStack() }
        btnNotification?.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, NotificationsFragment())
                .addToBackStack(null)
                .commit()
        }

        val conversations = ChatRepository.getConversations()

        if (conversations.isEmpty()) {
            layoutEmpty?.visibility = View.VISIBLE
            rvConversations?.visibility = View.GONE
        } else {
            layoutEmpty?.visibility = View.GONE
            rvConversations?.visibility = View.VISIBLE
        }

        messagesAdapter = MessagesAdapter(conversations) { selectedConv ->
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, ChatDetailFragment.newInstance(selectedConv.id, selectedConv.providerName, selectedConv.providerCategory, selectedConv.avatarName))
                .addToBackStack(null)
                .commit()
        }

        rvConversations?.layoutManager = LinearLayoutManager(requireContext())
        rvConversations?.adapter = messagesAdapter

        // Setup bottom navigation bar
        val bottomNav = view.findViewById<BottomNavigationView>(R.id.bottomNavigation)
        if (user?.role == User.ROLE_TECHNICIAN) {
            bottomNav?.menu?.clear()
            bottomNav?.inflateMenu(R.menu.technician_bottom_menu)
        } else {
            bottomNav?.selectedItemId = R.id.nav_messages
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
                    R.id.nav_messages -> true
                    R.id.nav_profile -> {
                        parentFragmentManager.beginTransaction()
                            .replace(R.id.fragment_container, SettingsFragment())
                            .commit()
                        true
                    }
                    else -> false
                }
            }
        }
    }
}
