package com.example.firstappformanda // <- SESUAIKAN

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

class TodoPagerAdapter(fragmentActivity: FragmentActivity) : FragmentStateAdapter(fragmentActivity) {

    override fun getItemCount(): Int = 2 // Kita punya 2 tab

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> MyTasksFragment() // Tab pertama untuk tugas kita
            1 -> PartnerTasksFragment() // Tab kedua untuk tugas pasangan
            else -> throw IllegalStateException("Invalid position $position")
        }
    }
}