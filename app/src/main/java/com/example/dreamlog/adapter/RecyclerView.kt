package com.example.dreamlog.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.dreamlog.databinding.ItemDreamBinding
import com.example.dreamlog.model.Dream

class DreamAdapter(private val dreams: List<Dream>) : RecyclerView.Adapter<DreamAdapter.DreamViewHolder>() {

    class DreamViewHolder(val binding: ItemDreamBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DreamViewHolder {
        val binding = ItemDreamBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return DreamViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DreamViewHolder, position: Int) {
        val dream = dreams[position]
        holder.binding.apply {
            textDream.text = dream.dreamText
            textEmotion.text = dream.emotion
            // 일단 꿈 내용이랑 감정만 표시
        }
    }

    // 전체 아이템 개수 반환
    override fun getItemCount(): Int = dreams.size
}
