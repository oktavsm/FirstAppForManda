package com.example.firstappformanda // <- SESUAIKAN

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

class AuthActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_auth)

        val tabLayout: TabLayout = findViewById(R.id.tab_layout_auth)
        val viewPager: ViewPager2 = findViewById(R.id.view_pager_auth)

        viewPager.adapter = AuthPagerAdapter(this)

        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "Login"
                1 -> "Register"
                else -> null
            }
        }.attach()
    }
}
