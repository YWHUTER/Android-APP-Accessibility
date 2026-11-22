package com.example.homework;

import com.example.homework.util.A11y;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.homework.data.SessionManager;
import com.example.homework.data.UserRepository;
import com.example.homework.util.RemoteImageLoader;

public class SettingsActivity extends AppCompatActivity {
    private UserRepository userRepository;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        userRepository = new UserRepository(this);
        sessionManager = new SessionManager(this);

        findViewById(R.id.btn_settings_back).setOnClickListener(v -> finish());
        findViewById(R.id.row_change_password).setOnClickListener(v -> showChangePasswordDialog());
        findViewById(R.id.row_clear_image_cache).setOnClickListener(v -> clearImageCache());
    }

    private void showChangePasswordDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_change_password, null);
        EditText etOldPassword = dialogView.findViewById(R.id.et_old_password);
        EditText etNewPassword = dialogView.findViewById(R.id.et_new_password);
        EditText etConfirmPassword = dialogView.findViewById(R.id.et_confirm_password);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("修改密码")
                .setView(dialogView)
                .setPositiveButton("确认", null)
                .setNegativeButton("取消", null)
                .create();
        dialog.setOnShowListener(dialogInterface ->
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                    String oldPassword = etOldPassword.getText().toString().trim();
                    String newPassword = etNewPassword.getText().toString().trim();
                    String confirmPassword = etConfirmPassword.getText().toString().trim();

                    if (TextUtils.isEmpty(oldPassword) || TextUtils.isEmpty(newPassword)) {
                        A11y.toast(this, "密码不能为空。");
                        return;
                    }
                    if (!newPassword.equals(confirmPassword)) {
                        A11y.toast(this, "两次输入的新密码不一致。");
                        return;
                    }
                    if (newPassword.length() < 4) {
                        A11y.toast(this, "新密码至少需要 4 个字符。");
                        return;
                    }

                    boolean success = userRepository.updatePassword(
                            sessionManager.getLoggedInUserId(), oldPassword, newPassword);
                    if (success) {
                        A11y.toast(this, "密码修改成功。");
                        dialog.dismiss();
                    } else {
                        A11y.toast(this, "旧密码不正确，请重试。");
                    }
                }));
        dialog.show();
    }

    private void clearImageCache() {
        RemoteImageLoader.clearMemoryCache();
        A11y.toast(this, "已清理图片缓存，已保留已发布的商品图片。");
    }
}
