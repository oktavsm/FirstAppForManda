package com.example.firstappformanda // <- SESUAIKAN

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter

// Adapter ini harus menerima Fragment sebagai parent, bukan FragmentActivity
class MonitoringPagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {

    override fun getItemCount(): Int = 2

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> MyMonitoringFragment() // Tab pertama untuk input data diri
            1 -> PartnerMonitoringFragment() // Tab kedua untuk lihat data pasangan
            else -> throw IllegalStateException("Invalid position $position")
        }
    }
}
