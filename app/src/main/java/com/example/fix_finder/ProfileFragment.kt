package com.example.fix_finder

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.fix_finder.data.model.ServiceProvider
import com.example.fix_finder.data.repository.ChatRepository
import com.example.fix_finder.data.repository.ResultState
import com.example.fix_finder.data.repository.ServiceRepository
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch

class ProfileFragment : Fragment() {

    private lateinit var serviceRepository: ServiceRepository
    private var providerId: String = "tech_1"

    companion object {
        private const val ARG_PROVIDER_ID = "provider_id"

        fun newInstance(providerId: String): ProfileFragment {
            val fragment = ProfileFragment()
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
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        serviceRepository = ServiceRepository()

        val btnBack = view.findViewById<ImageView>(R.id.btnBack)
        val btnShare = view.findViewById<ImageView>(R.id.btnShare)
        val btnBookNow = view.findViewById<MaterialButton>(R.id.btnBookNow)
        val btnRequestQuote = view.findViewById<MaterialButton>(R.id.btnRequestQuote)

        btnBack?.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        btnShare?.setOnClickListener {
            Toast.makeText(context, "Sharing technician profile link...", Toast.LENGTH_SHORT).show()
        }

        btnBookNow?.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, ServiceRequestFragment.newInstance(providerId))
                .addToBackStack(null)
                .commit()
        }

        btnRequestQuote?.setOnClickListener {
            lifecycleScope.launch {
                when (val result = serviceRepository.getProviderById(providerId)) {
                    is ResultState.Success -> {
                        val provider = result.data
                        val conv = ChatRepository.getOrCreateConversation(provider)
                        parentFragmentManager.beginTransaction()
                            .replace(R.id.fragment_container, ChatDetailFragment.newInstance(conv.id, conv.providerName, conv.providerCategory, conv.avatarName))
                            .addToBackStack(null)
                            .commit()
                    }
                    is ResultState.Error -> {
                        Toast.makeText(context, result.message, Toast.LENGTH_SHORT).show()
                    }
                    ResultState.Loading -> {}
                }
            }
        }

        // Fetch Provider Details from API
        lifecycleScope.launch {
            when (val result = serviceRepository.getProviderById(providerId)) {
                is ResultState.Success -> {
                    val provider = result.data
                    bindProviderData(view, provider)
                }
                is ResultState.Error -> {
                    Toast.makeText(context, result.message, Toast.LENGTH_SHORT).show()
                }
                ResultState.Loading -> {}
            }
        }
    }

    private fun bindProviderData(view: View, provider: ServiceProvider) {
        view.findViewById<TextView>(R.id.tvProfileName)?.text = provider.name
        view.findViewById<TextView>(R.id.tvBio)?.text = provider.bio
        view.findViewById<TextView>(R.id.tvAboutHeader)?.text = "About ${provider.name}"
        view.findViewById<TextView>(R.id.tvRatingReviews)?.text = "${provider.rating} (${provider.reviewsCount} reviews)  •  ${provider.yearsExperience} yrs exp."
        view.findViewById<TextView>(R.id.tvCategoryTag)?.text = "🔧 ${provider.categoryName}"
        view.findViewById<TextView>(R.id.tvLocationTag)?.text = "📍 ${provider.location}"
        view.findViewById<TextView>(R.id.tvJobsCount)?.text = provider.jobsCompleted.toString()
        view.findViewById<TextView>(R.id.tvResponseTime)?.text = "Responds in ${provider.responseTime}"

        val avatarRes = when (provider.avatarName) {
            "avatar_thabo" -> R.drawable.avatar_thabo
            "avatar_sipho" -> R.drawable.avatar_sipho
            "avatar_lerato" -> R.drawable.avatar_lerato
            else -> R.drawable.avatar_thabo
        }
        view.findViewById<ImageView>(R.id.imgAvatar)?.setImageResource(avatarRes)
    }
}
