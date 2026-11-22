package com.example.homework;

import com.example.homework.util.A11y;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.homework.data.SessionManager;
import com.example.homework.data.UserRepository;
import com.example.homework.model.User;

public class LoginActivity extends AppCompatActivity {
    private UserRepository userRepository;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        userRepository = new UserRepository(this);
        sessionManager = new SessionManager(this);

        if (sessionManager.isLoggedIn()) {
            openMain();
            return;
        }

        EditText etUsername = findViewById(R.id.et_username);
        EditText etPassword = findViewById(R.id.et_password);
        Button btnLogin = findViewById(R.id.btn_login);
        TextView tvRegister = findViewById(R.id.tv_register);
        TextView tvForgotPassword = findViewById(R.id.tv_forgot_password);
        TextView btnSsoLogin = findViewById(R.id.btn_sso_login);

        btnLogin.setOnClickListener(v -> {
            String username = etUsername.getText().toString().trim();
            String password = etPassword.getText().toString().trim();
            if (TextUtils.isEmpty(username) || TextUtils.isEmpty(password)) {
                A11y.toast(this, "请输入账号和密码。");
                return;
            }

            User user = userRepository.login(username, password);
            if (user == null) {
                A11y.toast(this, "账号或密码错误。");
                return;
            }

            sessionManager.login(user.getId());
            openMain();
        });

        tvRegister.setOnClickListener(v -> startActivity(new Intent(this, RegisterActivity.class)));

        tvForgotPassword.setOnClickListener(v -> showResetPasswordDialog(etUsername));

        btnSsoLogin.setText("校园账号一键登录");
        btnSsoLogin.setOnClickListener(v -> showCampusAuthDialog());
    }

    private void openMain() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }

    private void showResetPasswordDialog(EditText usernameSource) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_reset_password, null);
        EditText etResetUsername = dialogView.findViewById(R.id.et_reset_username);
        EditText etResetPassword = dialogView.findViewById(R.id.et_reset_password);
        EditText etResetConfirmPassword = dialogView.findViewById(R.id.et_reset_confirm_password);

        String currentUsername = usernameSource.getText().toString().trim();
        if (!TextUtils.isEmpty(currentUsername)) {
            etResetUsername.setText(currentUsername);
            etResetUsername.setSelection(etResetUsername.getText().length());
        }

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("重置密码")
                .setView(dialogView)
                .setPositiveButton("保存新密码", null)
                .setNegativeButton("取消", null)
                .create();
        dialog.setOnShowListener(dialogInterface ->
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                    String username = etResetUsername.getText().toString().trim();
                    String newPassword = etResetPassword.getText().toString().trim();
                    String confirmPassword = etResetConfirmPassword.getText().toString().trim();

                    if (TextUtils.isEmpty(username)) {
                        A11y.toast(this, "请输入要重置的校园账号。");
                        return;
                    }
                    if (!userRepository.usernameExists(username)) {
                        A11y.toast(this, "没有找到这个账号。");
                        return;
                    }
                    if (newPassword.length() < 4) {
                        A11y.toast(this, "新密码至少需要 4 个字符。");
                        return;
                    }
                    if (!newPassword.equals(confirmPassword)) {
                        A11y.toast(this, "两次输入的新密码不一致。");
                        return;
                    }

                    if (userRepository.resetPassword(username, newPassword)) {
                        usernameSource.setText(username);
                        A11y.toast(this, "密码已重置，请使用新密码登录。");
                        dialog.dismiss();
                    } else {
                        A11y.toast(this, "重置失败，请稍后再试。");
                    }
                }));
        dialog.show();
    }

    private void showCampusAuthDialog() {
        String[] labels = {
                "陈雅 alex · 数字媒体艺术",
                "林泽 mia · 计算机科学",
                "苏晚 leo · 工业设计"
        };
        String[] usernames = {"alex", "mia", "leo"};

        new AlertDialog.Builder(this)
                .setTitle("校园统一认证")
                .setItems(labels, (dialog, which) -> {
                    User user = userRepository.getByUsername(usernames[which]);
                    if (user == null) {
                        A11y.toast(this, "演示账号不存在，请先注册账号。");
                        return;
                    }
                    sessionManager.login(user.getId());
                    A11y.toast(this, "认证成功，欢迎回来。");
                    openMain();
                })
                .setNegativeButton("取消", null)
                .show();
    }
}
