package com.example.homework;

import com.example.homework.util.A11y;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import com.example.homework.data.CommunityRepository;
import com.example.homework.data.SessionManager;
import com.example.homework.model.Post;
import com.example.homework.util.ImageStorageHelper;
import com.example.homework.util.RemoteImageLoader;

import java.util.List;
import java.util.Locale;

public class CommunityActivity extends AppCompatActivity {
    private CommunityRepository communityRepository;
    private SessionManager sessionManager;
    private LinearLayout layoutPosts;
    private TextView tvStats;
    private String pendingPostImagePath;
    private ImageView pendingPostImagePreview;
    private TextView pendingPostImageAction;

    private final ActivityResultLauncher<String> postImagePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), this::handlePostImageSelected);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_community);

        communityRepository = new CommunityRepository(this);
        sessionManager = new SessionManager(this);
        layoutPosts = findViewById(R.id.layout_community_posts);
        tvStats = findViewById(R.id.tv_community_stats);

        findViewById(R.id.btn_community_back).setOnClickListener(v -> finish());
        findViewById(R.id.btn_new_post).setOnClickListener(v -> showNewPostDialog());

        loadPosts();
    }

    private void showNewPostDialog() {
        pendingPostImagePath = null;
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_new_post, null);
        EditText etContent = dialogView.findViewById(R.id.et_post_content);
        pendingPostImageAction = dialogView.findViewById(R.id.tv_select_post_image);
        pendingPostImagePreview = dialogView.findViewById(R.id.iv_selected_post_image);

        View.OnClickListener imagePickerListener = v -> postImagePickerLauncher.launch("image/*");
        pendingPostImageAction.setOnClickListener(imagePickerListener);
        pendingPostImagePreview.setOnClickListener(imagePickerListener);
        pendingPostImagePreview.setOnLongClickListener(v -> {
            pendingPostImagePath = null;
            pendingPostImagePreview.setVisibility(View.GONE);
            pendingPostImageAction.setText("添加配图");
            A11y.toast(this, "已移除配图。");
            return true;
        });

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("发布新帖子")
                .setView(dialogView)
                .setPositiveButton("发布", null)
                .setNegativeButton("取消", null)
                .create();
        dialog.setOnShowListener(dialogInterface ->
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                    String content = etContent.getText().toString().trim();
                    if (TextUtils.isEmpty(content)) {
                        A11y.toast(this, "帖子内容不能为空。");
                        return;
                    }

                    long postId = communityRepository.addPost(
                            sessionManager.getLoggedInUserId(), content, pendingPostImagePath);
                    if (postId > 0) {
                        A11y.toast(this, "帖子已发布！");
                        dialog.dismiss();
                        loadPosts();
                    } else {
                        A11y.toast(this, "发布失败，请稍后再试。");
                    }
                }));
        dialog.show();
    }

    private void loadPosts() {
        List<Post> posts = communityRepository.getAllPosts();
        long currentUserId = sessionManager.getLoggedInUserId();

        tvStats.setText(String.format(Locale.getDefault(),
                "社区共有 %d 条帖子，和同学们一起交流吧！", posts.size()));

        layoutPosts.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);

        for (Post post : posts) {
            View card = inflater.inflate(R.layout.item_community_post, layoutPosts, false);

            TextView tvAvatar = card.findViewById(R.id.tv_post_avatar);
            TextView tvUserName = card.findViewById(R.id.tv_post_user_name);
            TextView tvUserMajor = card.findViewById(R.id.tv_post_user_major);
            TextView tvTime = card.findViewById(R.id.tv_post_time);
            TextView tvContent = card.findViewById(R.id.tv_post_content);
            ImageView ivImage = card.findViewById(R.id.iv_post_image);
            Button btnDelete = card.findViewById(R.id.btn_delete_post);

            String avatarText = post.getUserName() == null || post.getUserName().isEmpty()
                    ? "帖" : post.getUserName().substring(0, 1);
            tvAvatar.setText(avatarText);
            tvUserName.setText(post.getUserName());
            tvUserMajor.setText(post.getUserMajor());
            tvTime.setText(DateUtils.getRelativeTimeSpanString(
                    post.getCreatedAt(),
                    System.currentTimeMillis(),
                    DateUtils.MINUTE_IN_MILLIS));
            tvContent.setText(post.getContent());

            if (!TextUtils.isEmpty(post.getImageUrl())) {
                ivImage.setVisibility(View.VISIBLE);
                RemoteImageLoader.load(ivImage, post.getImageUrl());
            } else {
                ivImage.setVisibility(View.GONE);
            }

            if (post.getUserId() == currentUserId) {
                btnDelete.setVisibility(View.VISIBLE);
                btnDelete.setOnClickListener(v -> new AlertDialog.Builder(this)
                        .setTitle("删除帖子")
                        .setMessage("确定要删除这条帖子吗？")
                        .setPositiveButton("删除", (dialog, which) -> {
                            communityRepository.deletePost(post.getId());
                            A11y.toast(this, "帖子已删除。");
                            loadPosts();
                        })
                        .setNegativeButton("取消", null)
                        .show());
            }

            layoutPosts.addView(card);
        }

        if (posts.isEmpty()) {
            TextView emptyView = new TextView(this);
            emptyView.setText("还没有帖子，来发布第一条吧！");
            emptyView.setTextColor(getColor(R.color.text_secondary));
            emptyView.setGravity(Gravity.CENTER);
            emptyView.setPadding(0, 72, 0, 72);
            layoutPosts.addView(emptyView);
        }
    }

    private void handlePostImageSelected(Uri uri) {
        if (uri == null) {
            return;
        }

        String savedPath = ImageStorageHelper.copyToInternalStorage(this, uri);
        if (TextUtils.isEmpty(savedPath)) {
            A11y.toast(this, "配图保存失败，请重试。");
            return;
        }

        pendingPostImagePath = savedPath;
        if (pendingPostImagePreview != null) {
            pendingPostImagePreview.setVisibility(View.VISIBLE);
            RemoteImageLoader.load(pendingPostImagePreview, savedPath);
        }
        if (pendingPostImageAction != null) {
            pendingPostImageAction.setText("更换配图");
        }
    }
}
