package com.example.firstappformanda // <- SESUAIKAN

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

class MonitoringContainerFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_monitoring_container, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Hubungkan komponen UI dari layout
        val tabLayout: TabLayout = view.findViewById(R.id.tab_layout_monitoring)
        val viewPager: ViewPager2 = view.findViewById(R.id.view_pager_monitoring)

        // Buat dan pasang adapter baru kita
        // Perhatikan: kita pakai 'this' karena kita berada di dalam Fragment
        viewPager.adapter = MonitoringPagerAdapter(this)

        // Hubungkan TabLayout dengan ViewPager dan atur judul untuk setiap tab
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "Kondisiku" // Tab pertama
                1 -> "Kondisi Pasangan" // Tab kedua
                else -> null
            }
        }.attach()
    }
}
