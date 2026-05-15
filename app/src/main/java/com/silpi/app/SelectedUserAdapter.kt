package com.silpi.app

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.imageview.ShapeableImageView

class SelectedUserAdapter(
        private val selectedUsers: MutableList<SelectableUser>,
        private val onRemoveUser: (SelectableUser) -> Unit
) : RecyclerView.Adapter<SelectedUserAdapter.SelectedUserViewHolder>() {

    class SelectedUserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageViewProfile: ShapeableImageView = itemView.findViewById(R.id.imageViewSelectedProfile)
        val buttonRemove: ImageButton = itemView.findViewById(R.id.buttonRemoveSelectedUser)
        val textViewUserName: TextView = itemView.findViewById(R.id.textViewSelectedUserName)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SelectedUserViewHolder {
        val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_selected_user, parent, false)
        return SelectedUserViewHolder(view)
    }

    override fun onBindViewHolder(holder: SelectedUserViewHolder, position: Int) {
        val user = selectedUsers[position]

        ProfileImageHelper.setProfileImage(holder.imageViewProfile, user.profileImageData)
        holder.textViewUserName.text = user.userName
        holder.buttonRemove.setOnClickListener {
            onRemoveUser(user)
        }
    }

    override fun getItemCount(): Int = selectedUsers.size
}
