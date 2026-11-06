package com.example.fix_finder

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.fix_finder.data.model.ServiceProvider
import com.example.fix_finder.data.repository.ResultState
import com.example.fix_finder.data.repository.ServiceRepository
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.ChipGroup
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SearchFragment : Fragment() {

    private lateinit var serviceRepository: ServiceRepository
    private lateinit var providerAdapter: ServiceProviderAdapter

    private var currentCategory: String? = null
    private var currentQuery: String? = null
    private var searchDebounceJob: Job? = null

    companion object {
        private const val ARG_CATEGORY = "arg_category"

        fun newInstance(category: String? = null): SearchFragment {
            val fragment = SearchFragment()
            if (category != null) {
                val args = Bundle()
                args.putString(ARG_CATEGORY, category)
                fragment.arguments = args
            }
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        currentCategory = arguments?.getString(ARG_CATEGORY)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_search, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        serviceRepository = ServiceRepository()

        val btnBack = view.findViewById<ImageView>(R.id.btnBack)
        val btnNotification = view.findViewById<ImageView>(R.id.btnNotification)
        val etSearchQuery = view.findViewById<EditText>(R.id.etSearchQuery)
        val btnClearSearch = view.findViewById<ImageView>(R.id.btnClearSearch)
        val chipGroupCategories = view.findViewById<ChipGroup>(R.id.chipGroupCategories)
        val pbLoading = view.findViewById<ProgressBar>(R.id.pbSearchLoading)
        val layoutEmpty = view.findViewById<LinearLayout>(R.id.layoutSearchEmpty)
        val layoutError = view.findViewById<LinearLayout>(R.id.layoutSearchError)
        val tvErrorMessage = view.findViewById<TextView>(R.id.tvSearchErrorMessage)
        val btnRetry = view.findViewById<MaterialButton>(R.id.btnRetrySearch)
        val rvProviders = view.findViewById<RecyclerView>(R.id.rvProviders)

        btnBack?.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        btnNotification?.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, NotificationsFragment())
                .addToBackStack(null)
                .commit()
        }

        btnClearSearch?.setOnClickListener {
            etSearchQuery?.setText("")
        }

        // Setup RecyclerView Adapter
        providerAdapter = ServiceProviderAdapter(
            providers = emptyList(),
            onViewProfileClick = { provider ->
                parentFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, ProfileFragment.newInstance(provider.id))
                    .addToBackStack(null)
                    .commit()
            },
            onQuickBookClick = { provider ->
                parentFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, ServiceRequestFragment.newInstance(provider.id))
                    .addToBackStack(null)
                    .commit()
            }
        )

        rvProviders?.layoutManager = LinearLayoutManager(requireContext())
        rvProviders?.adapter = providerAdapter

        // Pre-select category chip if passed in arguments
        val initCategory = currentCategory?.lowercase()
        when (initCategory) {
            "plumbing" -> chipGroupCategories?.check(R.id.chipPlumbing)
            "electrical" -> chipGroupCategories?.check(R.id.chipElectrical)
            "appliances" -> chipGroupCategories?.check(R.id.chipAppliances)
            "ac", "air conditioning" -> chipGroupCategories?.check(R.id.chipAC)
            "electronics" -> chipGroupCategories?.check(R.id.chipElectronics)
            "automotive" -> chipGroupCategories?.check(R.id.chipAutomotive)
            "handyman" -> chipGroupCategories?.check(R.id.chipHandyman)
            else -> chipGroupCategories?.check(R.id.chipAll)
        }

        chipGroupCategories?.setOnCheckedStateChangeListener { _, checkedIds ->
            currentCategory = when (checkedIds.firstOrNull()) {
                R.id.chipPlumbing -> "plumbing"
                R.id.chipElectrical -> "electrical"
                R.id.chipAppliances -> "appliances"
                R.id.chipAC -> "ac"
                R.id.chipElectronics -> "electronics"
                R.id.chipAutomotive -> "automotive"
                R.id.chipHandyman -> "handyman"
                else -> null
            }
            performSearch()
        }

        // Search text input listener with debounce
        etSearchQuery?.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val text = s?.toString()?.trim().orEmpty()
                btnClearSearch?.visibility = if (text.isNotEmpty()) View.VISIBLE else View.GONE
                searchDebounceJob?.cancel()
                searchDebounceJob = lifecycleScope.launch {
                    delay(300)
                    currentQuery = text.ifEmpty { null }
                    performSearch()
                }
            }
        })

        etSearchQuery?.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                currentQuery = etSearchQuery.text.toString().trim().ifEmpty { null }
                performSearch()
                true
            } else false
        }

        btnRetry?.setOnClickListener {
            performSearch()
        }

        // Bottom Navigation Bar
        val bottomNav = view.findViewById<BottomNavigationView>(R.id.bottomNavigation)
        bottomNav?.selectedItemId = R.id.nav_search
        bottomNav?.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    parentFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, HomeFragment())
                        .commit()
                    true
                }
                R.id.nav_search -> true
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

        // Initial search execution
        performSearch()
    }

    private fun performSearch() {
        val pbLoading = view?.findViewById<ProgressBar>(R.id.pbSearchLoading)
        val layoutEmpty = view?.findViewById<LinearLayout>(R.id.layoutSearchEmpty)
        val layoutError = view?.findViewById<LinearLayout>(R.id.layoutSearchError)
        val tvErrorMessage = view?.findViewById<TextView>(R.id.tvSearchErrorMessage)
        val rvProviders = view?.findViewById<RecyclerView>(R.id.rvProviders)

        if (providerAdapter.itemCount == 0) {
            pbLoading?.visibility = View.VISIBLE
            rvProviders?.visibility = View.GONE
        }
        layoutEmpty?.visibility = View.GONE
        layoutError?.visibility = View.GONE

        lifecycleScope.launch {
            when (val result = serviceRepository.getProviders(query = currentQuery, category = currentCategory)) {
                is ResultState.Success -> {
                    pbLoading?.visibility = View.GONE
                    val providers = result.data
                    if (providers.isEmpty()) {
                        layoutEmpty?.visibility = View.VISIBLE
                        rvProviders?.visibility = View.GONE
                        providerAdapter.updateData(emptyList())
                    } else {
                        layoutEmpty?.visibility = View.GONE
                        rvProviders?.visibility = View.VISIBLE
                        providerAdapter.updateData(providers)
                    }
                }
                is ResultState.Error -> {
                    pbLoading?.visibility = View.GONE
                    if (providerAdapter.itemCount == 0) {
                        rvProviders?.visibility = View.GONE
                        layoutError?.visibility = View.VISIBLE
                        tvErrorMessage?.text = result.message
                    }
                }
                ResultState.Loading -> {}
            }
        }
    }
}
