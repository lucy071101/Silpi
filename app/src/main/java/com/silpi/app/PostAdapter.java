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

    // 파이어베이스에서 게시글 목록을 받아와서 새로고침하는 기능
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

        // 🌟 [추가] 파이어스토어 문서에서 실제 저장된 댓글 개수(commentCount)를 꺼내옵니다!
        Long commentCount = doc.getLong("commentCount");

        // 숫자로 된 가짜 글과 Timestamp로 된 진짜 글을 모두 안전하게 가져오는 마법의 코드
        Long finalTime = 0L;
        Object timestampObj = doc.get("timestamp");

        if (timestampObj instanceof Number) {
            // 우리가 만든 가짜 글 (숫자 1)
            finalTime = ((Number) timestampObj).longValue();
        } else if (timestampObj instanceof com.google.firebase.Timestamp) {
            // 팀원이 만든 진짜 글 (Firebase 시간 객체)
            finalTime = ((com.google.firebase.Timestamp) timestampObj).toDate().getTime();
        }

        holder.tvTitle.setText(title != null ? title : "제목 없음");
        holder.tvContent.setText(content != null ? content : "내용이 없습니다.");
        holder.tvAuthor.setText("익명");

        // 시간 계산기에 안전하게 변환된 시간을 넣습니다!
        holder.tvTime.setText(formatTimeString(finalTime));

        holder.tvRecommend.setText("👍 " + (recommendCount != null ? recommendCount : 0));

        // 🌟 [핵심 변경] 더미 데이터 "5" 대신, 파이어베이스에서 가져온 진짜 댓글 개수를 넣어줍니다!
        // 만약 데이터가 비어있으면(null) 안전하게 0개로 표시합니다.
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

    // 방금 전, 몇 분 전 등 시간을 예쁘게 계산해 주는 마법의 도구
    private String formatTimeString(Long timestamp) {
        if (timestamp == null) return "알 수 없음";

        long currentTime = System.currentTimeMillis();
        long diffTime = (currentTime - timestamp) / 1000; // 초 단위로 변환

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