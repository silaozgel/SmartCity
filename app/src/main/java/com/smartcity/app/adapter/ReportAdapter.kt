package com.smartcity.app.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.button.MaterialButton
import com.smartcity.app.R
import com.smartcity.app.model.Report
import android.graphics.BitmapFactory
import android.util.Base64

class ReportAdapter(
    private val isAdmin: Boolean,
    private val onUpdateClick: (Report) -> Unit
) : RecyclerView.Adapter<ReportAdapter.ReportViewHolder>() {

    private var reportList = listOf<Report>()

    fun setReports(reports: List<Report>) {
        reportList = reports
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReportViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_report, parent, false)
        return ReportViewHolder(view)
    }

    override fun onBindViewHolder(holder: ReportViewHolder, position: Int) {
        holder.bind(reportList[position])
    }

    override fun getItemCount(): Int = reportList.size

    inner class ReportViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvTitle: TextView = itemView.findViewById(R.id.tvReportTitle)
        private val tvCategory: TextView = itemView.findViewById(R.id.tvReportCategory)
        private val tvStatus: TextView = itemView.findViewById(R.id.tvReportStatus)
        private val btnUpdateStatus: MaterialButton = itemView.findViewById(R.id.btnUpdateStatus)
        private val ivReportImage: ImageView = itemView.findViewById(R.id.ivReportImage)

        fun bind(report: Report) {
            tvTitle.text = report.title
            tvCategory.text = "${report.category} - ${report.address}"
            tvStatus.text = "Status: ${report.status}"

            if (isAdmin) {
                btnUpdateStatus.visibility = View.VISIBLE
                btnUpdateStatus.setOnClickListener {
                    onUpdateClick(report)
                }
            } else {
                btnUpdateStatus.visibility = View.GONE
            }

            //byte array kısmı
            if (report.imageUrl.isNotEmpty()) {
                ivReportImage.visibility = View.VISIBLE
                try {
                    // Metni tekrar Byte array çevirir
                    val imageBytes = Base64.decode(report.imageUrl, Base64.DEFAULT)
                    val decodedImage = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)

                    Glide.with(itemView.context)
                        .load(decodedImage)
                        .into(ivReportImage)
                } catch (e: Exception) {
                    ivReportImage.visibility = View.GONE
                }
            } else {
                ivReportImage.visibility = View.GONE
            }
        }
    }
}