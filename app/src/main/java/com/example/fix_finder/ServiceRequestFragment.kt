package com.example.fix_finder

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.fix_finder.data.api.ApiClient
import com.example.fix_finder.data.local.SessionManager
import com.example.fix_finder.data.model.ServiceProvider
import com.example.fix_finder.data.model.ServiceRequestCreate
import com.example.fix_finder.data.repository.ResultState
import com.example.fix_finder.data.repository.ServiceRepository
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class ServiceRequestFragment : Fragment() {

    private lateinit var serviceRepository: ServiceRepository
    private lateinit var sessionManager: SessionManager

    private var providerId: String = "tech_1"
    private var currentProvider: ServiceProvider? = null
    private var selectedImageUriString: String? = null

    private val calendar = Calendar.getInstance()

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            selectedImageUriString = uri.toString()
            view?.findViewById<ImageView>(R.id.imgReportPreview)?.apply {
                setImageURI(uri)
                visibility = View.VISIBLE
            }
            view?.findViewById<MaterialButton>(R.id.btnAttachPhoto)?.text = getString(R.string.photo_attached)
        }
    }

    companion object {
        private const val ARG_PROVIDER_ID = "provider_id"

        fun newInstance(providerId: String): ServiceRequestFragment {
            val fragment = ServiceRequestFragment()
            val args = Bundle()
            args.putString(ARG_PROVIDER_ID, providerId)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        providerId = arguments?.getString(ARG_PROVIDER_ID) ?: "tech_1"
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_service_request, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        serviceRepository = ServiceRepository(ApiClient.apiService, requireContext())
        sessionManager = SessionManager.getInstance(requireContext())

        // Trigger background offline sync if connectivity is available
        lifecycleScope.launch {
            serviceRepository.syncPendingOfflineReports()
        }

        val btnBack = view.findViewById<ImageView>(R.id.btnBack)
        val imgProviderAvatar = view.findViewById<ImageView>(R.id.imgProviderAvatar)
        val tvProviderName = view.findViewById<TextView>(R.id.tvProviderName)
        val tvProviderCategory = view.findViewById<TextView>(R.id.tvProviderCategory)

        val spServices = view.findViewById<Spinner>(R.id.spServices)
        val etDescription = view.findViewById<TextInputEditText>(R.id.etDescription)
        val etAddress = view.findViewById<TextInputEditText>(R.id.etAddress)
        val etDate = view.findViewById<TextInputEditText>(R.id.etDate)
        val etTime = view.findViewById<TextInputEditText>(R.id.etTime)

        val tilDescription = view.findViewById<TextInputLayout>(R.id.tilDescription)
        val tilAddress = view.findViewById<TextInputLayout>(R.id.tilAddress)

        val btnAttachPhoto = view.findViewById<MaterialButton>(R.id.btnAttachPhoto)
        val tvPriceEstimate = view.findViewById<TextView>(R.id.tvPriceEstimate)
        val tvReqError = view.findViewById<TextView>(R.id.tvReqError)
        val pbReqLoading = view.findViewById<ProgressBar>(R.id.pbReqLoading)
        val btnSubmitRequest = view.findViewById<MaterialButton>(R.id.btnSubmitRequest)

        btnBack?.setOnClickListener { parentFragmentManager.popBackStack() }

        btnAttachPhoto?.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        // Set default user address if logged in
        val user = sessionManager.getUser()
        etAddress?.setText(user?.location ?: "Polokwane, Limpopo")

        // Set default date & time
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())

        calendar.add(Calendar.DAY_OF_YEAR, 1)
        etDate?.setText(dateFormat.format(calendar.time))

        calendar.set(Calendar.HOUR_OF_DAY, 9)
        calendar.set(Calendar.MINUTE, 0)
        etTime?.setText(timeFormat.format(calendar.time))

        // Date Picker Dialog
        etDate?.setOnClickListener {
            val datePicker = DatePickerDialog(
                requireContext(),
                { _, year, month, dayOfMonth ->
                    calendar.set(Calendar.YEAR, year)
                    calendar.set(Calendar.MONTH, month)
                    calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                    etDate.setText(dateFormat.format(calendar.time))
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            )
            datePicker.datePicker.minDate = System.currentTimeMillis()
            datePicker.show()
        }

        // Time Picker Dialog
        etTime?.setOnClickListener {
            val timePicker = TimePickerDialog(
                requireContext(),
                { _, hourOfDay, minute ->
                    calendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
                    calendar.set(Calendar.MINUTE, minute)
                    etTime.setText(timeFormat.format(calendar.time))
                },
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                false
            )
            timePicker.show()
        }

        etDescription?.addTextChangedListener { tilDescription?.error = null; tvReqError?.visibility = View.GONE }
        etAddress?.addTextChangedListener { tilAddress?.error = null; tvReqError?.visibility = View.GONE }

        // Fetch Provider Details
        lifecycleScope.launch {
            pbReqLoading?.visibility = View.VISIBLE
            when (val result = serviceRepository.getProviderById(providerId)) {
                is ResultState.Success -> {
                    pbReqLoading?.visibility = View.GONE
                    val provider = result.data
                    currentProvider = provider

                    tvProviderName?.text = provider.name
                    tvProviderCategory?.text = "${provider.categoryName} Professional"

                    val avatarRes = getAvatarDrawableRes(provider.avatarName)
                    imgProviderAvatar?.setImageResource(avatarRes)

                    // Populate Services Spinner
                    val serviceNames = if (provider.services.isNotEmpty()) {
                        provider.services.map { "${it.name} (${it.priceDisplay})" }
                    } else {
                        listOf("General ${provider.categoryName} Repair (${provider.startingPriceDisplay})")
                    }

                    val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, serviceNames)
                    spServices?.adapter = adapter

                    spServices?.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                        override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                            if (provider.services.isNotEmpty() && position < provider.services.size) {
                                tvPriceEstimate?.text = provider.services[position].priceDisplay
                            } else {
                                tvPriceEstimate?.text = provider.startingPriceDisplay
                            }
                        }
                        override fun onNothingSelected(parent: AdapterView<*>?) {}
                    }
                }
                is ResultState.Error -> {
                    pbReqLoading?.visibility = View.GONE
                    tvReqError?.text = result.message
                    tvReqError?.visibility = View.VISIBLE
                }
                ResultState.Loading -> {}
            }
        }

        // Submit Request Button
        btnSubmitRequest?.setOnClickListener {
            val provider = currentProvider
            val description = etDescription?.text?.toString()?.trim().orEmpty()
            val address = etAddress?.text?.toString()?.trim().orEmpty()
            val date = etDate?.text?.toString()?.trim().orEmpty()
            val time = etTime?.text?.toString()?.trim().orEmpty()

            var isValid = true
            if (description.isBlank()) {
                tilDescription?.error = "Please describe the problem in detail"
                isValid = false
            }
            if (address.isBlank()) {
                tilAddress?.error = "Please enter the service location"
                isValid = false
            }

            if (!isValid) return@setOnClickListener

            val selectedServiceIndex = spServices?.selectedItemPosition ?: 0
            val selectedService = provider?.services?.getOrNull(selectedServiceIndex)
            val serviceName = selectedService?.name ?: "General ${provider?.categoryName ?: "Fix"} Service"
            val priceEst = selectedService?.priceDisplay ?: provider?.startingPriceDisplay ?: "R350"

            val currentUser = sessionManager.getUser()
            val requestPayload = ServiceRequestCreate(
                userId = currentUser?.id ?: "usr_demo",
                userName = currentUser?.name ?: "Customer",
                providerId = provider?.id ?: providerId,
                providerName = provider?.name ?: "Technician",
                providerCategory = provider?.categoryName ?: "Service",
                serviceName = serviceName,
                description = description,
                address = address,
                date = date,
                time = time,
                estimatedPrice = priceEst,
                imageUrl = selectedImageUriString
            )

            pbReqLoading?.visibility = View.VISIBLE
            btnSubmitRequest.isEnabled = false
            tvReqError?.visibility = View.GONE

            lifecycleScope.launch {
                when (val result = serviceRepository.createServiceRequest(requestPayload)) {
                    is ResultState.Success -> {
                        pbReqLoading?.visibility = View.GONE
                        btnSubmitRequest.isEnabled = true
                        val createdReq = result.data

                        val alertMsg = if (createdReq.isSynced) {
                            "Your request #${createdReq.id} for ${createdReq.serviceName} has been sent to ${createdReq.providerName}.\n\nYou will be notified when ${createdReq.providerName} accepts the job."
                        } else {
                            "You are currently offline. Your request #${createdReq.id} was saved locally and will automatically synchronize when connection returns."
                        }

                        AlertDialog.Builder(requireContext())
                            .setTitle(if (createdReq.isSynced) "Request Submitted!" else "Saved Offline!")
                            .setMessage(alertMsg)
                            .setPositiveButton("View My Bookings") { _, _ ->
                                parentFragmentManager.beginTransaction()
                                    .replace(R.id.fragment_container, BookingsFragment())
                                    .commit()
                            }
                            .setNegativeButton("Back to Home") { _, _ ->
                                parentFragmentManager.beginTransaction()
                                    .replace(R.id.fragment_container, HomeFragment())
                                    .commit()
                            }
                            .setCancelable(false)
                            .show()
                    }
                    is ResultState.Error -> {
                        pbReqLoading?.visibility = View.GONE
                        btnSubmitRequest.isEnabled = true
                        tvReqError?.text = result.message
                        tvReqError?.visibility = View.VISIBLE
                    }
                    ResultState.Loading -> {}
                }
            }
        }
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
