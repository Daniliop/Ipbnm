package com.example.doctorschedule

import android.app.AlertDialog
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DoctorAdapter(private val doctors: List<Doctor>) :
    RecyclerView.Adapter<DoctorAdapter.DoctorHolder>() {

    class DoctorHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val clinicTv: TextView = itemView.findViewById(R.id.tv_clinic)
        val nameTv: TextView = itemView.findViewById(R.id.tv_doctor_name)
        val specialtyTv: TextView = itemView.findViewById(R.id.tv_doctor_specialty)
        val schedulesContainer: ViewGroup = itemView.findViewById(R.id.schedules_container)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DoctorHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_doctor, parent, false)
        return DoctorHolder(view)
    }

    override fun onBindViewHolder(holder: DoctorHolder, position: Int) {
        val doctor = doctors[position]
        holder.clinicTv.text = doctor.clinic
        holder.nameTv.text = doctor.name
        holder.specialtyTv.text = doctor.specialty

        holder.schedulesContainer.removeAllViews()
        for (schedule in doctor.schedules) {
            val scheduleView = LayoutInflater.from(holder.itemView.context)
                .inflate(R.layout.item_schedule, holder.schedulesContainer, false)
            val text = "${schedule.day} (${schedule.time})"
            scheduleView.findViewById<TextView>(R.id.tv_schedule_text).text = text
            scheduleView.setOnClickListener {
                fetchAndShowTurnInfo(holder.itemView.context, schedule)
            }
            holder.schedulesContainer.addView(scheduleView)
        }
    }

    override fun getItemCount() = doctors.size

    private fun fetchAndShowTurnInfo(context: android.content.Context, schedule: Schedule) {
        val progressDialog = AlertDialog.Builder(context)
            .setMessage("در حال دریافت اطلاعات...")
            .setCancelable(false)
            .create()
        progressDialog.show()

        CoroutineScope(Dispatchers.IO).launch {
            val info = TurnInfoService.getTurnInfo(schedule.showId)
            withContext(Dispatchers.Main) {
                progressDialog.dismiss()
                if (info != null) {
                    // ذخیرهٔ جزئیات نوبت در فایل JSON
                    JsonStorage.saveTurnInfo(
                        context,
                        schedule.showId,
                        info.appoint,
                        info.telAppoint,
                        info.webAppoint
                    )
                    showTurnInfoDialog(context, schedule, info)
                } else {
                    AlertDialog.Builder(context)
                        .setMessage("خطا در دریافت اطلاعات")
                        .setPositiveButton("بستن", null)
                        .show()
                }
            }
        }
    }

    private fun showTurnInfoDialog(context: android.content.Context, schedule: Schedule, info: TurnInfoService.TurnInfo) {
        val dialogView = LayoutInflater.from(context)
            .inflate(R.layout.dialog_turn_info, null)
        val titleView: TextView = dialogView.findViewById(R.id.tv_turn_title)
        val appointValue: TextView = dialogView.findViewById(R.id.tv_appoint_value)
        val telValue: TextView = dialogView.findViewById(R.id.tv_tel_value)
        val webValue: TextView = dialogView.findViewById(R.id.tv_web_value)
        titleView.text = "${schedule.day}، ${schedule.time}"
        appointValue.text = info.appoint.toString()
        telValue.text = info.telAppoint.toString()
        webValue.text = info.webAppoint.toString()
        AlertDialog.Builder(context)
            .setView(dialogView)
            .setPositiveButton("بسیار خُب") { dialog, _ -> dialog.dismiss() }
            .show()
    }
}
