package com.example.homework;

import com.example.homework.util.A11y;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;

import androidx.appcompat.app.AppCompatActivity;

import com.example.homework.data.SessionManager;
import com.example.homework.data.UserRepository;
import com.example.homework.model.User;

public class EditProfileActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        UserRepository userRepository = new UserRepository(this);
        SessionManager sessionManager = new SessionManager(this);

        EditText etName = findViewById(R.id.et_edit_name);
        Spinner spMajor = findViewById(R.id.sp_edit_major);
        EditText etInterests = findViewById(R.id.et_edit_interests);
        EditText etBio = findViewById(R.id.et_edit_bio);
        Button btnSave = findViewById(R.id.btn_save_profile);

        long userId = sessionManager.getLoggedInUserId();
        User user = userRepository.getById(userId);
        if (user == null) {
            A11y.toast(this, "无法加载用户信息。");
            finish();
            return;
        }

        String[] majors = {"计算机科学与技术", "软件工程", "通信工程", "电子信息工程", "自动化", "机械设计制造及其自动化", "工商管理", "会计学", "法学", "汉语言文学", "英语", "数学与应用数学", "物理学", "其他专业"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, majors);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spMajor.setAdapter(adapter);

        etName.setText(user.getName());
        int majorIndex = -1;
        if (user.getMajor() != null) {
            for (int i = 0; i < majors.length; i++) {
                if (majors[i].equals(user.getMajor())) {
                    majorIndex = i;
                    break;
                }
            }
        }
        if (majorIndex >= 0) {
            spMajor.setSelection(majorIndex);
        } else if (user.getMajor() != null && !user.getMajor().isEmpty()) {
            spMajor.setSelection(majors.length - 1); // Select "其他专业" if not found
        }
        etInterests.setText(user.getInterests());
        etBio.setText(user.getBio());

        findViewById(R.id.btn_edit_profile_back).setOnClickListener(v -> finish());

        btnSave.setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            String major = spMajor.getSelectedItem().toString();
            if (TextUtils.isEmpty(name)) {
                A11y.toast(this, "姓名不能为空。");
                return;
            }

            user.setName(name);
            user.setMajor(major);
            user.setInterests(etInterests.getText().toString().trim());
            user.setBio(etBio.getText().toString().trim());

            if (userRepository.updateProfile(user)) {
                A11y.toast(this, "资料已更新。");
                finish();
            } else {
                A11y.toast(this, "保存失败，请稍后再试。");
            }
        });
    }
}
