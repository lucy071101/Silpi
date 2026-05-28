package com.silpi.app

import android.graphics.BitmapFactory
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ChatAdapter(
        private val messageList: MutableList<ChatMessage>,
        private val myUserId: String,
        private val profileImagesByUserId: Map<String, String>,
        private val onImageClick: (String) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var lastAnimatedPosition = -1

    companion object {
        private const val VIEW_TYPE_SYSTEM = 0
        private const val VIEW_TYPE_LEFT = 1
        private const val VIEW_TYPE_RIGHT = 2
        private const val VIEW_TYPE_DATE = 3
    }

    class LeftViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageViewSenderProfile: ImageView = itemView.findViewById(R.id.imageViewSenderProfile)
        val textViewSenderNameLeft: TextView = itemView.findViewById(R.id.textViewSenderNameLeft)
        val textViewMessageLeft: TextView = itemView.findViewById(R.id.textViewMessageLeft)
        val imageViewMessageLeft: ImageView = itemView.findViewById(R.id.imageViewMessageLeft)
        val textViewTimeLeft: TextView = itemView.findViewById(R.id.textViewTimeLeft)
    }

    class RightViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textViewMessageRight: TextView = itemView.findViewById(R.id.textViewMessageRight)
        val imageViewMessageRight: ImageView = itemView.findViewById(R.id.imageViewMessageRight)
        val textViewTimeRight: TextView = itemView.findViewById(R.id.textViewTimeRight)
    }

    class SystemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textViewSystemMessage: TextView = itemView.findViewById(R.id.textViewSystemMessage)
    }

    class DateViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textViewDateDivider: TextView = itemView.findViewById(R.id.textViewDateDivider)
    }

    override fun getItemViewType(position: Int): Int {
        if (messageList[position].messageType == "date") {
            return VIEW_TYPE_DATE
        }

        if (messageList[position].messageType == "system") {
            return VIEW_TYPE_SYSTEM
        }

        return if (messageList[position].senderId == myUserId) {
            VIEW_TYPE_RIGHT
        } else {
            VIEW_TYPE_LEFT
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_DATE -> {
                val view = LayoutInflater.from(parent.context)
                        .inflate(R.layout.item_chat_date_divider, parent, false)
                DateViewHolder(view)
            }
            VIEW_TYPE_SYSTEM -> {
                val view = LayoutInflater.from(parent.context)
                        .inflate(R.layout.item_chat_system, parent, false)
                SystemViewHolder(view)
            }
            VIEW_TYPE_RIGHT -> {
                val view = LayoutInflater.from(parent.context)
                        .inflate(R.layout.item_chat_right, parent, false)
                RightViewHolder(view)
            }
            else -> {
                val view = LayoutInflater.from(parent.context)
                        .inflate(R.layout.item_chat_left, parent, false)
                LeftViewHolder(view)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val currentPosition = holder.bindingAdapterPosition
        if (currentPosition == RecyclerView.NO_POSITION) return

        val chatMessage = messageList[currentPosition]
        if (holder is DateViewHolder) {
            holder.textViewDateDivider.text = chatMessage.message
            return
        }

        if (holder is SystemViewHolder) {
            holder.textViewSystemMessage.text = chatMessage.message
            return
        }

        val formattedTime = formatTimestamp(chatMessage.timestamp)

        val isSameSenderAsPrevious =
                currentPosition > 0 &&
                        messageList[currentPosition - 1].messageType == "text" &&
                        messageList[currentPosition - 1].senderId == chatMessage.senderId

        val shouldShowTime =
                currentPosition == messageList.lastIndex ||
                        messageList[currentPosition + 1].messageType != "text" ||
                        messageList[currentPosition + 1].senderId != chatMessage.senderId

        if (holder is RightViewHolder) {
            bindMessageContent(
                    textView = holder.textViewMessageRight,
                    imageView = holder.imageViewMessageRight,
                    chatMessage = chatMessage
            )

            if (shouldShowTime) {
                holder.textViewTimeRight.visibility = View.VISIBLE
                holder.textViewTimeRight.text = formattedTime
            } else {
                holder.textViewTimeRight.visibility = View.GONE
            }

        } else if (holder is LeftViewHolder) {
            ProfileImageHelper.setProfileImage(
                    holder.imageViewSenderProfile,
                    profileImagesByUserId[chatMessage.senderId].orEmpty()
            )

            bindMessageContent(
                    textView = holder.textViewMessageLeft,
                    imageView = holder.imageViewMessageLeft,
                    chatMessage = chatMessage
            )

            val shouldShowSenderName =
                    currentPosition == 0 ||
                            messageList[currentPosition - 1].messageType != "text" ||
                            messageList[currentPosition - 1].senderId != chatMessage.senderId

            if (shouldShowSenderName) {
                holder.textViewSenderNameLeft.visibility = View.VISIBLE
                holder.textViewSenderNameLeft.text = chatMessage.senderName
            } else {
                holder.textViewSenderNameLeft.visibility = View.GONE
            }

            if (shouldShowTime) {
                holder.textViewTimeLeft.visibility = View.VISIBLE
                holder.textViewTimeLeft.text = formattedTime
            } else {
                holder.textViewTimeLeft.visibility = View.GONE
            }
        }

        setMessageTopMargin(holder.itemView, isSameSenderAsPrevious)

        if (currentPosition > lastAnimatedPosition) {
            val animation = AnimationUtils.loadAnimation(
                    holder.itemView.context,
                    R.anim.item_slide_in
            )
            holder.itemView.startAnimation(animation)
            lastAnimatedPosition = currentPosition
        }
    }

    override fun getItemCount(): Int {
        return messageList.size
    }

    private fun bindMessageContent(
            textView: TextView,
            imageView: ImageView,
            chatMessage: ChatMessage
    ) {
        if (chatMessage.messageType == "image" && chatMessage.imageData.isNotBlank()) {
            textView.visibility = View.GONE
            imageView.visibility = View.VISIBLE

            val imageBytes = Base64.decode(chatMessage.imageData, Base64.DEFAULT)
            val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
            imageView.setImageBitmap(bitmap)
            imageView.setOnClickListener {
                onImageClick(chatMessage.imageData)
            }
        } else {
            imageView.visibility = View.GONE
            imageView.setOnClickListener(null)
            textView.visibility = View.VISIBLE
            textView.text = chatMessage.message
        }
    }

    private fun formatTimestamp(timestamp: Long): String {
        return ChatTimeHelper.formatChatTime(timestamp)
    }

    private fun setMessageTopMargin(itemView: View, isSameSenderAsPrevious: Boolean) {
        val layoutParams = itemView.layoutParams as RecyclerView.LayoutParams

        val smallMargin = dpToPx(itemView, 1)
        val largeMargin = dpToPx(itemView, 8)

        layoutParams.topMargin = if (isSameSenderAsPrevious) smallMargin else largeMargin
        itemView.layoutParams = layoutParams
    }

    private fun dpToPx(view: View, dp: Int): Int {
        val density = view.context.resources.displayMetrics.density
        return (dp * density).toInt()
    }
}
