package com.example.dreamlog.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.dreamlog.databinding.ItemDreamBinding
import com.example.dreamlog.model.Dream
import java.text.SimpleDateFormat
import java.util.*

class DreamAdapter(private val dreams: List<Dream>) : RecyclerView.Adapter<DreamAdapter.DreamViewHolder>() {

    class DreamViewHolder(val binding: ItemDreamBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DreamViewHolder {
        val binding = ItemDreamBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return DreamViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DreamViewHolder, position: Int) {
        val dream = dreams[position]
        holder.binding.apply {
            // timestamp → Date 변환
            val sdf = SimpleDateFormat("yyyy.MM.dd", Locale.getDefault())
            textDate.text = sdf.format(dream.timestamp.toDate())

            // 꿈 내용
            textDream.text = dream.dreamText

            // 감정 매핑
            textEmotion.text = when (dream.emotion) {
                "Happy" -> "😊 행복"
                "Sad" -> "😢 슬픔"
                "Angry" -> "😠 분노"
                "Surprise" -> "😲 놀람"
                "Fear" -> "😨 공포"
                "Disgust" -> "🤢 역겨움"
                else -> "😶 중립"
            }

            // GPT 해석
            textGptInterpretation.text = dream.gptInterpretation

            // 이미지 표시 (imageUrl 있을 때만)
            if (!dream.imageUrl.isNullOrEmpty()) {
                imageDream.visibility = View.VISIBLE
                Glide.with(imageDream.context)
                    .load(dream.imageUrl)
                    .centerCrop()
                    .into(imageDream)
            } else {
                imageDream.visibility = View.GONE
            }
        }
    }

    override fun getItemCount(): Int = dreams.size
}
