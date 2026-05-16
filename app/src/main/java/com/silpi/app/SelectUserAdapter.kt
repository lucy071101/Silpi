package com.silpi.app

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.imageview.ShapeableImageView

class SelectUserAdapter(
        private val userList: MutableList<SelectableUser>,
        private val onSelectionChanged: () -> Unit
) : RecyclerView.Adapter<SelectUserAdapter.SelectUserViewHolder>() {

    class SelectUserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageViewProfile: ShapeableImageView = itemView.findViewById(R.id.imageViewProfile)
        val checkBoxUser: CheckBox = itemView.findViewById(R.id.checkBoxUser)
        val textViewUserName: TextView = itemView.findViewById(R.id.textViewUserName)
        val textViewUserId: TextView = itemView.findViewById(R.id.textViewUserId)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SelectUserViewHolder {
        val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_select_user, parent, false)
        return SelectUserViewHolder(view)
    }

    override fun onBindViewHolder(holder: SelectUserViewHolder, position: Int) {
        val user = userList[position]

        ProfileImageHelper.setProfileImage(holder.imageViewProfile, user.profileImageData)
        holder.textViewUserName.text = user.userName
        holder.textViewUserId.text = user.email.toHandle()

        holder.checkBoxUser.setOnCheckedChangeListener(null)
        holder.checkBoxUser.isChecked = user.isSelected

        holder.checkBoxUser.setOnCheckedChangeListener { _, isChecked ->
            user.isSelected = isChecked
            onSelectionChanged()
        }

        holder.itemView.setOnClickListener {
            user.isSelected = !user.isSelected
            notifyItemChanged(position)
            onSelectionChanged()
        }
    }

    override fun getItemCount(): Int = userList.size

    fun getSelectedUsers(): List<SelectableUser> {
        return userList.filter { it.isSelected }
    }

    private fun String.toHandle(): String {
        val handle = substringBefore("@").trim()
        return if (handle.isNotEmpty()) "@$handle" else ""
    }
}
