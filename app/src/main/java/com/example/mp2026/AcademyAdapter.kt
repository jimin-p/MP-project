package com.example.mp2026

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class AcademyAdapter(private var academyList: List<Academy>) :
    RecyclerView.Adapter<AcademyAdapter.AcademyViewHolder>() {

    class AcademyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvAcademyName: TextView = itemView.findViewById(R.id.tvAcademyName)
        val tvCategoryDistance: TextView = itemView.findViewById(R.id.tvCategoryDistance)
        val tvSubjects: TextView = itemView.findViewById(R.id.tvSubjects)
        val tvRating: TextView = itemView.findViewById(R.id.tvRating)
        val tvPrice: TextView = itemView.findViewById(R.id.tvPrice)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AcademyViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_academy_card, parent, false)
        return AcademyViewHolder(view)
    }

    override fun onBindViewHolder(holder: AcademyViewHolder, position: Int) {
        val academy = academyList[position]
        holder.tvAcademyName.text = academy.name
        holder.tvCategoryDistance.text = "${academy.category} · ${academy.address}"
        holder.tvSubjects.text = academy.curriculum
        holder.tvRating.text = "★ ${academy.rating}"
        holder.tvPrice.text = academy.price
    }

    override fun getItemCount(): Int = academyList.size

    // 상단 카테고리를 눌렀을 때 리스트를 갱신해 주는 함수
    fun updateList(newList: List<Academy>) {
        academyList = newList
        notifyDataSetChanged()
    }
}