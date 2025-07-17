package com.example.firstappformanda // <- SESUAIKAN

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class OnboardingAdapter(private val onboardingItems: List<OnboardingItem>) :
    RecyclerView.Adapter<OnboardingAdapter.OnboardingViewHolder>() {

    inner class OnboardingViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val image = view.findViewById<ImageView>(R.id.iv_onboarding_image)
        private val title = view.findViewById<TextView>(R.id.tv_onboarding_title)
        private val description = view.findViewById<TextView>(R.id.tv_onboarding_description)

        fun bind(onboardingItem: OnboardingItem) {
            image.setImageResource(onboardingItem.image)
            title.text = onboardingItem.title
            description.text = onboardingItem.description
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OnboardingViewHolder {
        return OnboardingViewHolder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.onboarding_item_container, parent, false
            )
        )
    }

    override fun onBindViewHolder(holder: OnboardingViewHolder, position: Int) {
        holder.bind(onboardingItems[position])
    }

    override fun getItemCount(): Int = onboardingItems.size
}