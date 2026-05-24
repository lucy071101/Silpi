package com.silpi.app;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.firestore.DocumentSnapshot;
import java.util.ArrayList;
import java.util.List;

public class PostAdapter extends RecyclerView.Adapter<PostAdapter.PostViewHolder> {

    private List<DocumentSnapshot> postList = new ArrayList<>();

    public void setPosts(List<DocumentSnapshot> posts) {
        this.postList = posts;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public PostViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_post, parent, false);
        return new PostViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PostViewHolder holder, int position) {
        DocumentSnapshot doc = postList.get(position);

        String title = doc.getString("title");
        String content = doc.getString("content");
        Long recommendCount = doc.getLong("recommendCount");
        Long commentCount = doc.getLong("commentCount");

        String authorName = doc.getString("authorName");

        Long finalTime = 0L;
        Object timestampObj = doc.get("timestamp");

        if (timestampObj instanceof Number) {
            finalTime = ((Number) timestampObj).longValue();
        } else if (timestampObj instanceof com.google.firebase.Timestamp) {
            finalTime = ((com.google.firebase.Timestamp) timestampObj).toDate().getTime();
        }

        holder.tvTitle.setText(title != null ? title : "제목 없음");
        holder.tvContent.setText(content != null ? content : "내용이 없습니다.");

        holder.tvAuthor.setText(authorName != null ? authorName : "익명");

        holder.tvTime.setText(formatTimeString(finalTime));
        holder.tvRecommend.setText("👍 " + (recommendCount != null ? recommendCount : 0));
        holder.tvComment.setText("💬 " + (commentCount != null ? commentCount : 0));

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(v.getContext(), PostDetailActivity.class);
            intent.putExtra("postId", doc.getId());
            v.getContext().startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return postList.size();
    }

    class PostViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvContent, tvAuthor, tvTime, tvRecommend, tvComment;

        public PostViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tv_item_post_title);
            tvContent = itemView.findViewById(R.id.tv_item_post_content);
            tvAuthor = itemView.findViewById(R.id.tv_item_post_author);
            tvTime = itemView.findViewById(R.id.tv_item_post_time);
            tvRecommend = itemView.findViewById(R.id.tv_item_post_recommend);
            tvComment = itemView.findViewById(R.id.tv_item_post_comment);
        }
    }

    private String formatTimeString(Long timestamp) {
        if (timestamp == null) return "알 수 없음";

        long currentTime = System.currentTimeMillis();
        long diffTime = (currentTime - timestamp) / 1000;

        if (diffTime < 60) {
            return "방금 전";
        } else if (diffTime < 3600) {
            return (diffTime / 60) + "분 전";
        } else if (diffTime < 86400) {
            return (diffTime / 3600) + "시간 전";
        } else {
            return (diffTime / 86400) + "일 전";
        }
    }
}