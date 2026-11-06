package com.example.fix_finder

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.fix_finder.data.local.safeClick
import com.example.fix_finder.data.repository.ChatRepository
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ChatDetailFragment : Fragment() {

    private var conversationId: String = "conv_1"
    private var providerName: String = "Thabo Mokoena"
    private var providerCategory: String = "Plumbing Professional"
    private var avatarName: String = "avatar_thabo"

    private lateinit var chatAdapter: ChatAdapter

    companion object {
        private const val ARG_CONVERSATION_ID = "arg_conv_id"
        private const val ARG_PROVIDER_NAME = "arg_provider_name"
        private const val ARG_PROVIDER_CATEGORY = "arg_provider_category"
        private const val ARG_AVATAR_NAME = "arg_avatar_name"

        fun newInstance(
            conversationId: String,
            providerName: String,
            providerCategory: String,
            avatarName: String
        ): ChatDetailFragment {
            val fragment = ChatDetailFragment()
            val args = Bundle()
            args.putString(ARG_CONVERSATION_ID, conversationId)
            args.putString(ARG_PROVIDER_NAME, providerName)
            args.putString(ARG_PROVIDER_CATEGORY, providerCategory)
            args.putString(ARG_AVATAR_NAME, avatarName)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        conversationId = arguments?.getString(ARG_CONVERSATION_ID) ?: "conv_1"
        providerName = arguments?.getString(ARG_PROVIDER_NAME) ?: "Thabo Mokoena"
        providerCategory = arguments?.getString(ARG_PROVIDER_CATEGORY) ?: "Plumbing Professional"
        avatarName = arguments?.getString(ARG_AVATAR_NAME) ?: "avatar_thabo"
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_chat, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val btnBack = view.findViewById<ImageView>(R.id.btnBack)
        val imgFixerAvatar = view.findViewById<ImageView>(R.id.imgFixerAvatar)
        val tvFixerName = view.findViewById<TextView>(R.id.tvFixerName)
        val tvFixerStatus = view.findViewById<TextView>(R.id.tvFixerStatus)
        val btnCallFixer = view.findViewById<ImageView>(R.id.btnCallFixer)

        val rvMessages = view.findViewById<RecyclerView>(R.id.rvMessages)
        val etMessageInput = view.findViewById<EditText>(R.id.etMessageInput)
        val btnSendMessage = view.findViewById<MaterialButton>(R.id.btnSendMessage)

        btnBack?.safeClick { parentFragmentManager.popBackStack() }

        tvFixerName?.text = providerName
        tvFixerStatus?.text = "Online  •  $providerCategory"

        val avatarRes = when (avatarName) {
            "avatar_thabo" -> R.drawable.avatar_thabo
            "avatar_sipho" -> R.drawable.avatar_sipho
            "avatar_lerato" -> R.drawable.avatar_lerato
            else -> R.drawable.avatar_thabo
        }
        imgFixerAvatar?.setImageResource(avatarRes)

        btnCallFixer?.safeClick {
            val callIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:0825551234"))
            try {
                val pm = requireContext().packageManager
                if (callIntent.resolveActivity(pm) != null) {
                    startActivity(callIntent)
                } else {
                    Toast.makeText(context, "Dialer unavailable on this device", Toast.LENGTH_SHORT).show()
                }
            } catch (_: Exception) {
                Toast.makeText(context, "Unable to open dialer", Toast.LENGTH_SHORT).show()
            }
        }

        val messagesList = ChatRepository.getMessages(conversationId).toMutableList()
        chatAdapter = ChatAdapter(messagesList)

        val layoutManager = LinearLayoutManager(requireContext())
        layoutManager.stackFromEnd = true
        rvMessages?.layoutManager = layoutManager
        rvMessages?.adapter = chatAdapter

        btnSendMessage?.safeClick {
            val text = etMessageInput?.text?.toString()?.trim().orEmpty()
            if (text.isBlank()) return@safeClick

            etMessageInput?.setText("")
            val newMsg = ChatRepository.sendMessage(conversationId, text)
            messagesList.add(newMsg)
            chatAdapter.updateData(messagesList.toList())
            if (messagesList.isNotEmpty()) {
                rvMessages?.smoothScrollToPosition(messagesList.size - 1)
            }

            // Auto-reply simulation from fixer after 1s
            viewLifecycleOwner.lifecycleScope.launch {
                delay(1000)
                if (isAdded) {
                    val replyMsg = ChatRepository.generateAutoReply(conversationId, providerName)
                    messagesList.add(replyMsg)
                    chatAdapter.updateData(messagesList.toList())
                    if (messagesList.isNotEmpty()) {
                        rvMessages?.smoothScrollToPosition(messagesList.size - 1)
                    }
                }
            }
        }
    }
}
