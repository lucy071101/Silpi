package com.silpi.app;

import android.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.List;

public class CommentAdapter extends RecyclerView.Adapter<CommentAdapter.CommentViewHolder> {

    private List<DocumentSnapshot> commentList = new ArrayList<>();

    public void setComments(List<DocumentSnapshot> comments) {
        this.commentList = comments;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public CommentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_comment, parent, false);
        return new CommentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CommentViewHolder holder, int position) {
        DocumentSnapshot doc = commentList.get(position);

        String content = doc.getString("content");
        holder.tvContent.setText(content != null ? content : "");

        String authorName = doc.getString("authorName");
        holder.tvAuthor.setText(authorName != null ? authorName : "익명");

        Boolean isAnonymous = doc.getBoolean("isAnonymous");
        String authorProfile = doc.getString("authorProfile");

        if (Boolean.TRUE.equals(isAnonymous)) {
            holder.ivProfile.setImageResource(R.drawable.ic_default_profile);
        }
        else if (authorProfile != null && !authorProfile.isEmpty()) {
            if (authorProfile.startsWith("http")) {
                Glide.with(holder.itemView.getContext())
                        .load(authorProfile)
                        .placeholder(R.drawable.ic_default_profile)
                        .error(R.drawable.ic_default_profile)
                        .circleCrop()
                        .into(holder.ivProfile);
            } else {
                try {
                    byte[] decodedBytes = android.util.Base64.decode(authorProfile, android.util.Base64.DEFAULT);
                    Glide.with(holder.itemView.getContext())
                            .asBitmap()
                            .load(decodedBytes)
                            .placeholder(R.drawable.ic_default_profile)
                            .error(R.drawable.ic_default_profile)
                            .circleCrop()
                            .into(holder.ivProfile);
                } catch (IllegalArgumentException ex) {
                    holder.ivProfile.setImageResource(R.drawable.ic_default_profile);
                }
            }
        }
        else {
            holder.ivProfile.setImageResource(R.drawable.ic_default_profile);
        }

        String authorId = doc.getString("authorId");
        String myUserId = CurrentUserProvider.INSTANCE.userId(holder.itemView.getContext());
        String myName = CurrentUserProvider.INSTANCE.userName(holder.itemView.getContext());

        boolean isMyComment = false;
        if (authorId != null && authorId.equals(myUserId)) {
            isMyComment = true;
        } else if (authorId == null && authorName != null && authorName.equals(myName) && !authorName.equals("익명")) {
            isMyComment = true;
        }

        if (isMyComment) {
            holder.btnMore.setVisibility(View.VISIBLE);
            holder.btnMore.setOnClickListener(v -> {
                PopupMenu popup = new PopupMenu(holder.itemView.getContext(), holder.btnMore);
                popup.getMenu().add("수정");
                popup.getMenu().add("삭제");

                popup.setOnMenuItemClickListener(item -> {
                    if (item.getTitle().equals("수정")) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(holder.itemView.getContext());
                        builder.setTitle("댓글 수정");

                        final EditText input = new EditText(holder.itemView.getContext());
                        input.setText(content);
                        input.setSelection(input.getText().length());

                        FrameLayout container = new FrameLayout(holder.itemView.getContext());
                        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                        int margin = (int) (20 * holder.itemView.getResources().getDisplayMetrics().density);
                        params.setMargins(margin, margin, margin, margin);
                        input.setLayoutParams(params);
                        container.addView(input);
                        builder.setView(container);

                        builder.setPositiveButton("수정", (dialog, which) -> {
                            String newText = input.getText().toString().trim();
                            if (!newText.isEmpty()) {
                                doc.getReference().update("content", newText)
                                        .addOnSuccessListener(aVoid -> {
                                            Toast.makeText(holder.itemView.getContext(), "댓글이 수정되었습니다.", Toast.LENGTH_SHORT).show();
                                        });
                            } else {
                                Toast.makeText(holder.itemView.getContext(), "내용을 입력해주세요.", Toast.LENGTH_SHORT).show();
                            }
                        });

                        builder.setNegativeButton("취소", (dialog, which) -> dialog.cancel());
                        builder.show();

                    } else if (item.getTitle().equals("삭제")) {
                        new AlertDialog.Builder(holder.itemView.getContext())
                                .setTitle("댓글 삭제")
                                .setMessage("정말로 이 댓글을 삭제하시겠습니까?")
                                .setPositiveButton("삭제", (dialog, which) -> {
                                    doc.getReference().delete();
                                    String postId = doc.getReference().getParent().getParent().getId();
                                    FirebaseFirestore.getInstance().collection("posts").document(postId)
                                            .update("commentCount", FieldValue.increment(-1));
                                })
                                .setNegativeButton("취소", null).show();
                    }
                    return true;
                });
                popup.show();
            });
        } else {
            holder.btnMore.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return commentList.size();
    }

    class CommentViewHolder extends RecyclerView.ViewHolder {
        TextView tvAuthor, tvContent;
        ImageView ivProfile, btnMore;

        public CommentViewHolder(@NonNull View itemView) {
            super(itemView);
            tvAuthor = itemView.findViewById(R.id.tv_item_comment_author);
            tvContent = itemView.findViewById(R.id.tv_item_comment_content);
            ivProfile = itemView.findViewById(R.id.iv_item_comment_profile);
            btnMore = itemView.findViewById(R.id.btn_more_options);
        }
    }
}