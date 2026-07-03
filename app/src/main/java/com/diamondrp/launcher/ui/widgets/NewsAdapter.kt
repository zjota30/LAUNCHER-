package com.diamondrp.launcher.ui.widgets

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.diamondrp.launcher.databinding.ItemNewsBinding
import com.diamondrp.launcher.network.models.NewsItem

class NewsAdapter(
    private val onNewsClick: (NewsItem) -> Unit
) : ListAdapter<NewsItem, NewsAdapter.NewsViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NewsViewHolder {
        val binding = ItemNewsBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return NewsViewHolder(binding)
    }

    override fun onBindViewHolder(holder: NewsViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class NewsViewHolder(private val binding: ItemNewsBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: NewsItem) {
            binding.txtNewsTitle.text = item.title
            binding.txtNewsDate.text = item.date ?: ""
            if (!item.imageUrl.isNullOrBlank()) {
                binding.imgNews.load(item.imageUrl)
            }
            binding.root.setOnClickListener { onNewsClick(item) }
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<NewsItem>() {
        override fun areItemsTheSame(oldItem: NewsItem, newItem: NewsItem) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: NewsItem, newItem: NewsItem) = oldItem == newItem
    }
}
