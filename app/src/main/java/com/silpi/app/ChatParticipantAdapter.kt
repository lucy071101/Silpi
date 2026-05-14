package com.silpi.app

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.imageview.ShapeableImageView

class ChatParticipantAdapter(
        private val participants: List<ChatParticipant>
) : RecyclerView.Adapter<ChatParticipantAdapter.ChatParticipantViewHolder>() {

    class ChatParticipantViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageViewProfile: ShapeableImageView = itemView.findViewById(R.id.imageViewParticipantProfile)
        val textViewName: TextView = itemView.findViewById(R.id.textViewParticipantName)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatParticipantViewHolder {
        val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_chat_participant, parent, false)
        return ChatParticipantViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChatParticipantViewHolder, position: Int) {
        val participant = participants[position]
        ProfileImageHelper.setProfileImage(holder.imageViewProfile, participant.profileImageData)
        holder.textViewName.text = if (participant.isMe) {
            "${participant.userName} (나)"
        } else {
            participant.userName
        }
    }

    override fun getItemCount(): Int = participants.size
}
