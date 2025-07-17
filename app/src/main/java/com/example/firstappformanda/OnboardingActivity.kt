package com.example.firstappformanda // <- SESUAIKAN

import android.content.Intent
import android.os.Bundle
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.get
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.button.MaterialButton

class OnboardingActivity : AppCompatActivity() {

    private lateinit var onboardingAdapter: OnboardingAdapter
    private lateinit var viewPager: ViewPager2
    private lateinit var layoutIndicators: LinearLayout
    private lateinit var btnAction: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_onboarding)

        initializeViews()
        setupOnboardingItems()
        setupIndicators()
        setCurrentIndicator(0)

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                setCurrentIndicator(position)
            }
        })

        // Di dalam OnboardingActivity.kt -> onCreate()

        btnAction.setOnClickListener {
            if (viewPager.currentItem + 1 < onboardingAdapter.itemCount) {
                viewPager.currentItem += 1
            } else {
                // (BARU) Tandai bahwa onboarding sudah selesai
                val prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE)
                prefs.edit().putBoolean("onboarding_completed", true).apply()

                // Pindah ke halaman Auth
                startActivity(Intent(applicationContext, AuthActivity::class.java))
                finish()
            }
        }
    }

    private fun initializeViews() {
        layoutIndicators = findViewById(R.id.layout_indicators)
        btnAction = findViewById(R.id.btn_action)
    }

    private fun setupOnboardingItems() {
        val items = listOf(
            OnboardingItem(
                image = R.drawable.ic_monitoring, // Ganti dengan ikonmu
                title = "Monitoring Menyeluruh",
                description = "Pantau siklus, mood, dan catatan harian pasanganmu dengan mudah."
            ),
            OnboardingItem(
                image = R.drawable.ic_productivity, // Ganti dengan ikonmu
                title = "Produktivitas Bersama",
                description = "Atur jadwal dan tugas bersama, serta sinkronkan dengan Google Calendar."
            ),
            OnboardingItem(
                image = R.drawable.ic_chat, // Ganti dengan ikonmu
                title = "Tetap Terhubung",
                description = "Kirim pesan dan pengingat untuk menunjukkan perhatianmu setiap saat."
            )
        )
        onboardingAdapter = OnboardingAdapter(items)
        viewPager = findViewById(R.id.view_pager_onboarding)
        viewPager.adapter = onboardingAdapter
    }

    private fun setupIndicators() {
        val indicators = arrayOfNulls<ImageView>(onboardingAdapter.itemCount)
        val layoutParams: LinearLayout.LayoutParams =
            LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT)
        layoutParams.setMargins(8, 0, 8, 0)

        for (i in indicators.indices) {
            indicators[i] = ImageView(applicationContext)
            indicators[i]?.apply {
                this.setImageDrawable(
                    ContextCompat.getDrawable(applicationContext, R.drawable.indicator_inactive)
                )
                this.layoutParams = layoutParams
            }
            layoutIndicators.addView(indicators[i])
        }
    }

    private fun setCurrentIndicator(index: Int) {
        val childCount = layoutIndicators.childCount
        for (i in 0 until childCount) {
            val imageView = layoutIndicators[i] as ImageView
            if (i == index) {
                imageView.setImageDrawable(
                    ContextCompat.getDrawable(applicationContext, R.drawable.indicator_active)
                )
            } else {
                imageView.setImageDrawable(
                    ContextCompat.getDrawable(applicationContext, R.drawable.indicator_inactive)
                )
            }
        }
        // Ganti teks tombol jika di halaman terakhir
        if (index == onboardingAdapter.itemCount - 1) {
            btnAction.text = "Get Started"
        } else {
            btnAction.text = "Next"
        }
    }
}
