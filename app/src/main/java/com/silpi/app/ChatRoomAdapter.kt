package com.silpi.app

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.imageview.ShapeableImageView

class ChatRoomAdapter(
        private val chatRoomList: MutableList<ChatRoom>,
        private val myUserId: String,
        private val profileImagesByUserId: Map<String, String>,
        private val onRoomLongClick: (ChatRoom) -> Unit
) : RecyclerView.Adapter<ChatRoomAdapter.ChatRoomViewHolder>() {

    class ChatRoomViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val layoutGroupProfileCluster: View = itemView.findViewById(R.id.layoutGroupProfileCluster)
        val imageViewProfile: ShapeableImageView = itemView.findViewById(R.id.imageViewProfile)
        val imageGroupProfiles: List<ShapeableImageView> = listOf(
                itemView.findViewById(R.id.imageGroupProfile1),
                itemView.findViewById(R.id.imageGroupProfile2),
                itemView.findViewById(R.id.imageGroupProfile3),
                itemView.findViewById(R.id.imageGroupProfile4)
        )
        val textViewRoomName: TextView = itemView.findViewById(R.id.textViewRoomName)
        val textViewLastMessage: TextView = itemView.findViewById(R.id.textViewLastMessage)
        val textViewTime: TextView = itemView.findViewById(R.id.textViewTime)
        val layoutUnreadBadge: FrameLayout = itemView.findViewById(R.id.layoutUnreadBadge)
        val textViewUnreadCount: TextView = itemView.findViewById(R.id.textViewUnreadCount)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatRoomViewHolder {
        val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_chat_room, parent, false)
        return ChatRoomViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChatRoomViewHolder, position: Int) {
        val chatRoom = chatRoomList[position]
        val unread = chatRoom.unreadCount[myUserId] ?: 0

        holder.textViewRoomName.text = getDisplayRoomName(chatRoom)
        holder.textViewLastMessage.text = chatRoom.lastMessage.ifBlank { "대화를 시작해보세요" }
        holder.textViewTime.text = formatChatListTime(chatRoom.lastMessageTime)
        adjustTimePosition(holder, chatRoom)
        bindProfileImage(holder, chatRoom)

        if (unread > 0) {
            holder.layoutUnreadBadge.visibility = View.VISIBLE
            holder.textViewUnreadCount.text = unread.toString()
        } else {
            holder.layoutUnreadBadge.visibility = View.GONE
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
            chatRoom.roomName.ifBlank { "그룹 대화방" }
        } else {
            val otherUserId = chatRoom.participants.firstOrNull { it != myUserId }
            if (otherUserId != null) {
                chatRoom.participantNames[otherUserId] ?: chatRoom.roomName.ifBlank { "이름 없는 대화방" }
            } else {
                chatRoom.roomName.ifBlank { "이름 없는 대화방" }
            }
        }
    }

    private fun bindProfileImage(holder: ChatRoomViewHolder, chatRoom: ChatRoom) {
        if (chatRoom.group) {
            holder.imageViewProfile.visibility = View.INVISIBLE
            holder.layoutGroupProfileCluster.visibility = View.VISIBLE
            bindGroupProfileCluster(holder, chatRoom.participants.take(4))
            return
        }

        holder.layoutGroupProfileCluster.visibility = View.GONE
        holder.imageViewProfile.visibility = View.VISIBLE
        val otherUserId = chatRoom.participants.firstOrNull { it != myUserId }
        val profileImageData = profileImagesByUserId[otherUserId].orEmpty()
        ProfileImageHelper.setProfileImage(holder.imageViewProfile, profileImageData)
    }

    private fun adjustTimePosition(holder: ChatRoomViewHolder, chatRoom: ChatRoom) {
        val layoutParams = holder.textViewTime.layoutParams as RelativeLayout.LayoutParams
        layoutParams.topMargin = dpToPx(holder.itemView, if (chatRoom.group) 18 else 22)
        holder.textViewTime.layoutParams = layoutParams
    }

    private fun bindGroupProfileCluster(holder: ChatRoomViewHolder, participantIds: List<String>) {
        for (index in holder.imageGroupProfiles.indices) {
            val imageView = holder.imageGroupProfiles[index]
            val userId = participantIds.getOrNull(index)
            if (userId == null) {
                imageView.visibility = View.INVISIBLE
            } else {
                imageView.visibility = View.VISIBLE
                ProfileImageHelper.setProfileImage(imageView, profileImagesByUserId[userId].orEmpty())
            }
        }
    }

    private fun formatChatListTime(timestamp: Long): String {
        if (timestamp == 0L) return ""
        return ChatTimeHelper.formatChatListTime(timestamp)
    }

    private fun dpToPx(view: View, dp: Int): Int {
        val density = view.context.resources.displayMetrics.density
        return (dp * density).toInt()
    }
}
