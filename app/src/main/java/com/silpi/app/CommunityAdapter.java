package com.silpi.app;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.firestore.DocumentSnapshot;
import java.util.ArrayList;
import java.util.List;

public class CommunityAdapter extends RecyclerView.Adapter<CommunityAdapter.CommunityViewHolder> {

    private List<DocumentSnapshot> communityList = new ArrayList<>();


    public void setCommunities(List<DocumentSnapshot> communities) {
        this.communityList = communities;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public CommunityViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_community, parent, false);
        return new CommunityViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CommunityViewHolder holder, int position) {
        DocumentSnapshot doc = communityList.get(position);

        holder.tvRank.setText(String.valueOf(position + 1));

        String name = doc.getString("name");
        Long memberCount = doc.getLong("memberCount");

        holder.tvName.setText(name != null ? name : "이름 없는 모임");
        holder.tvMembers.setText("👥 참여 인원 " + (memberCount != null ? memberCount : 0) + "명");

        holder.itemView.setOnClickListener(v -> {
            android.widget.Toast.makeText(v.getContext(), name + " 게시판으로 이동합니다!", android.widget.Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public int getItemCount() {
        return communityList.size();
    }

    class CommunityViewHolder extends RecyclerView.ViewHolder {
        TextView tvRank, tvName, tvMembers;

        public CommunityViewHolder(@NonNull View itemView) {
            super(itemView);
            tvRank = itemView.findViewById(R.id.tv_community_rank);
            tvName = itemView.findViewById(R.id.tv_community_name);
            tvMembers = itemView.findViewById(R.id.tv_community_members);
        }
    }
}