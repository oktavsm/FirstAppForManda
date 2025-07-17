package com.example.firstappformanda // <- SESUAIKAN

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter

class ProductivityPagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {

    override fun getItemCount(): Int = 2

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> MyActivitiesFragment() // Tab pertama untuk jadwal/tugas kita
            1 -> PartnerActivitiesFragment() // Tab kedua untuk jadwal/tugas pasangan
            else -> throw IllegalStateException("Invalid position $position")
        }
    }
}
