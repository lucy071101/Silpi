package com.silpi.app

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ChatRoomAdapter(
        private val chatRoomList: MutableList<ChatRoom>,
        private val myUserId: String,
        private val profileImagesByUserId: Map<String, String>,
        private val onRoomLongClick: (ChatRoom) -> Unit
) : RecyclerView.Adapter<ChatRoomAdapter.ChatRoomViewHolder>() {

    class ChatRoomViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageViewProfile: ImageView = itemView.findViewById(R.id.imageViewProfile)
        val textViewRoomName: TextView = itemView.findViewById(R.id.textViewRoomName)
        val textViewLastMessage: TextView = itemView.findViewById(R.id.textViewLastMessage)
        val textViewTime: TextView = itemView.findViewById(R.id.textViewTime)
        val textViewUnreadCount: TextView = itemView.findViewById(R.id.textViewUnreadCount)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatRoomViewHolder {
        val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_chat_room, parent, false)
        return ChatRoomViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChatRoomViewHolder, position: Int) {
        val chatRoom = chatRoomList[position]

        val displayName = getDisplayRoomName(chatRoom)
        val lastMessageText = if (chatRoom.lastMessage.isBlank()) {
            "대화를 시작해보세요"
        } else {
            chatRoom.lastMessage
        }

        val unread = chatRoom.unreadCount[myUserId] ?: 0

        holder.textViewRoomName.text = displayName
        holder.textViewLastMessage.text = lastMessageText
        holder.textViewTime.text = formatChatListTime(chatRoom.lastMessageTime)
        bindProfileImage(holder.imageViewProfile, chatRoom)

        if (unread > 0) {
            holder.textViewUnreadCount.visibility = View.VISIBLE
            holder.textViewUnreadCount.text = unread.toString()
        } else {
            holder.textViewUnreadCount.visibility = View.GONE
        }

        holder.itemView.setOnClickListener {
            android.util.Log.d("ROOM_CLICK", "click roomId=${chatRoom.roomId}")
            val context = holder.itemView.context
            val intent = Intent(context, ChatActivity::class.java)
            intent.putExtra("chatRoomId", chatRoom.roomId)
            context.startActivity(intent)
        }

        holder.itemView.setOnLongClickListener {
            android.util.Log.d("ROOM_CLICK", "long click roomId=${chatRoom.roomId}")
            onRoomLongClick(chatRoom)
            true
        }
    }

    override fun getItemCount(): Int {
        return chatRoomList.size
    }

    private fun getDisplayRoomName(chatRoom: ChatRoom): String {
        return if (chatRoom.group) {
            if (chatRoom.roomName.isNotBlank()) chatRoom.roomName else "그룹 채팅방"
        } else {
            val otherUserId = chatRoom.participants.firstOrNull { it != myUserId }
            if (otherUserId != null) {
                chatRoom.participantNames[otherUserId] ?: chatRoom.roomName.ifBlank { "이름 없는 채팅방" }
            } else {
                chatRoom.roomName.ifBlank { "이름 없는 채팅방" }
            }
        }
    }

    private fun bindProfileImage(imageView: ImageView, chatRoom: ChatRoom) {
        if (chatRoom.group) {
            imageView.setImageResource(R.mipmap.ic_launcher_round)
            return
        }

        val otherUserId = chatRoom.participants.firstOrNull { it != myUserId }
        val profileImageData = profileImagesByUserId[otherUserId].orEmpty()
        ProfileImageHelper.setProfileImage(imageView, profileImageData)
    }

    private fun formatChatListTime(timestamp: Long): String {
        if (timestamp == 0L) return ""

        val formatter = SimpleDateFormat("a h:mm", Locale.KOREAN)
        return formatter.format(Date(timestamp))
    }
}
