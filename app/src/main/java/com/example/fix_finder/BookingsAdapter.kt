package com.example.fix_finder

import android.content.res.ColorStateList
import android.graphics.Color
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.fix_finder.data.local.safeClick
import com.example.fix_finder.data.model.ServiceRequest
import com.google.android.material.button.MaterialButton

class BookingsAdapter(
    private var requests: List<ServiceRequest>,
    private val isTechnicianView: Boolean = false,
    private val onAcceptClick: ((ServiceRequest) -> Unit)? = null,
    private val onDeclineClick: ((ServiceRequest) -> Unit)? = null
) : RecyclerView.Adapter<BookingsAdapter.BookingViewHolder>() {

    fun updateData(newRequests: List<ServiceRequest>) {
        val diffCallback = object : DiffUtil.Callback() {
            override fun getOldListSize(): Int = requests.size
            override fun getNewListSize(): Int = newRequests.size
            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return requests[oldItemPosition].id == newRequests[newItemPosition].id
            }
            override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return requests[oldItemPosition] == newRequests[newItemPosition]
            }
        }
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        this.requests = newRequests
        diffResult.dispatchUpdatesTo(this)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookingViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_service_request, parent, false)
        return BookingViewHolder(view)
    }

    override fun onBindViewHolder(holder: BookingViewHolder, position: Int) {
        try {
            holder.bind(requests[position], isTechnicianView, onAcceptClick, onDeclineClick)
        } catch (_: Throwable) {
            // Safe fallback preventing adapter crashes
        }
    }

    override fun getItemCount(): Int = requests.size

    class BookingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvRequestId: TextView? = itemView.findViewById(R.id.tvRequestId)
        private val tvSyncBadge: TextView? = itemView.findViewById(R.id.tvSyncBadge)
        private val tvStatusBadge: TextView? = itemView.findViewById(R.id.tvStatusBadge)
        private val tvServiceName: TextView? = itemView.findViewById(R.id.tvServiceName)
        private val tvProviderName: TextView? = itemView.findViewById(R.id.tvProviderName)
        private val tvDescription: TextView? = itemView.findViewById(R.id.tvDescription)
        private val tvDateTime: TextView? = itemView.findViewById(R.id.tvDateTime)
        private val tvAddress: TextView? = itemView.findViewById(R.id.tvAddress)
        private val tvEstimatedPrice: TextView? = itemView.findViewById(R.id.tvEstimatedPrice)
        private val imgAttachedPhoto: ImageView? = itemView.findViewById(R.id.imgAttachedPhoto)

        private val layoutTechActions: LinearLayout? = itemView.findViewById(R.id.layoutTechActions)
        private val btnAcceptJob: MaterialButton? = itemView.findViewById(R.id.btnAcceptJob)
        private val btnDeclineJob: MaterialButton? = itemView.findViewById(R.id.btnDeclineJob)

        fun bind(
            request: ServiceRequest,
            isTechnicianView: Boolean,
            onAcceptClick: ((ServiceRequest) -> Unit)?,
            onDeclineClick: ((ServiceRequest) -> Unit)?
        ) {
            tvRequestId?.text = "#${request.id}"
            tvServiceName?.text = request.serviceName
            tvProviderName?.text = "Technician: ${request.providerName} (${request.providerCategory})"
            tvDescription?.text = request.description
            tvDateTime?.text = "📅 ${request.date} at ${request.time}"
            tvAddress?.text = "📍 ${request.address}"
            tvEstimatedPrice?.text = request.estimatedPrice

            // Offline Sync Badge
            tvSyncBadge?.visibility = if (!request.isSynced) View.VISIBLE else View.GONE

            // Photo Attachment (Safely validated to prevent SecurityException/NPE)
            val imgUrl = request.imageUrl
            if (!imgUrl.isNullOrEmpty() && imgAttachedPhoto != null) {
                try {
                    val uri = Uri.parse(imgUrl)
                    val cr = itemView.context.applicationContext.contentResolver
                    val pfd = cr.openFileDescriptor(uri, "r")
                    if (pfd != null) {
                        pfd.close()
                        imgAttachedPhoto.setImageURI(uri)
                        imgAttachedPhoto.visibility = View.VISIBLE
                    } else {
                        imgAttachedPhoto.visibility = View.GONE
                    }
                } catch (_: Throwable) {
                    imgAttachedPhoto.visibility = View.GONE
                }
            } else {
                imgAttachedPhoto?.visibility = View.GONE
            }

            // Status Badge Formatting
            if (tvStatusBadge != null) {
                tvStatusBadge.text = request.status
                when (request.status) {
                    ServiceRequest.STATUS_ACCEPTED -> {
                        tvStatusBadge.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#DCFCE7"))
                        tvStatusBadge.setTextColor(Color.parseColor("#15803D"))
                    }
                    ServiceRequest.STATUS_IN_PROGRESS -> {
                        tvStatusBadge.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#DBEAFE"))
                        tvStatusBadge.setTextColor(Color.parseColor("#1D4ED8"))
                    }
                    ServiceRequest.STATUS_COMPLETED -> {
                        tvStatusBadge.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#E0E7FF"))
                        tvStatusBadge.setTextColor(Color.parseColor("#4338CA"))
                    }
                    ServiceRequest.STATUS_CANCELLED -> {
                        tvStatusBadge.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#FEE2E2"))
                        tvStatusBadge.setTextColor(Color.parseColor("#B91C1C"))
                    }
                    else -> { // PENDING
                        tvStatusBadge.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#FEF3C7"))
                        tvStatusBadge.setTextColor(Color.parseColor("#B45309"))
                    }
                }
            }

            // Technician Actions Row
            if (isTechnicianView && layoutTechActions != null) {
                layoutTechActions.visibility = View.VISIBLE
                if (request.status == ServiceRequest.STATUS_PENDING) {
                    btnAcceptJob?.text = "Accept Job"
                    btnDeclineJob?.visibility = View.VISIBLE
                } else if (request.status == ServiceRequest.STATUS_ACCEPTED) {
                    btnAcceptJob?.text = "Mark Completed"
                    btnDeclineJob?.visibility = View.GONE
                } else {
                    layoutTechActions.visibility = View.GONE
                }

                btnAcceptJob?.safeClick { onAcceptClick?.invoke(request) }
                btnDeclineJob?.safeClick { onDeclineClick?.invoke(request) }
            } else {
                layoutTechActions?.visibility = View.GONE
            }
        }
    }
}
