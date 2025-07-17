package com.example.firstappformanda // <- SESUAIKAN

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class WishlistAdapter(
    private val wishlist: List<WishlistItem>,
    private val onWishClickListener: (WishlistItem) -> Unit,
    private val onDeleteClickListener: (WishlistItem) -> Unit
) : RecyclerView.Adapter<WishlistAdapter.WishlistViewHolder>() {

    class WishlistViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val photo: ImageView = itemView.findViewById(R.id.iv_wish_photo)
        val placeholder: ImageView = itemView.findViewById(R.id.iv_placeholder_icon)
        val title: TextView = itemView.findViewById(R.id.tv_wish_title)
        val deleteButton: ImageView = itemView.findViewById(R.id.iv_delete_wish)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WishlistViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_wishlist, parent, false)
        return WishlistViewHolder(view)
    }

    override fun onBindViewHolder(holder: WishlistViewHolder, position: Int) {
        val item = wishlist[position]
        holder.title.text = item.title

        // Logika untuk menampilkan foto atau placeholder
        if (!item.imageUrl.isNullOrEmpty()) {
            // Jika ada URL gambar, muat dengan Glide
            holder.placeholder.visibility = View.GONE
            Glide.with(holder.itemView.context)
                .load(item.imageUrl)
                .into(holder.photo)
        } else {
            // Jika tidak ada URL, tampilkan placeholder
            holder.placeholder.visibility = View.VISIBLE
            holder.photo.setImageDrawable(null) // Kosongkan foto
        }

        // Beri aksi klik untuk seluruh kartu (untuk membuka detail)
        holder.itemView.setOnClickListener {
            onWishClickListener(item)
        }

        // Beri aksi klik untuk tombol hapus
        holder.deleteButton.setOnClickListener {
            onDeleteClickListener(item)
        }
    }

    override fun getItemCount(): Int = wishlist.size
}
