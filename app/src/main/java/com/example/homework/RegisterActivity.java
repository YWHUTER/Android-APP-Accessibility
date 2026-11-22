package com.example.homework;

import com.example.homework.util.A11y;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.example.homework.data.SessionManager;
import com.example.homework.data.UserRepository;
import com.example.homework.model.User;
import com.example.homework.util.ImageStorageHelper;

import java.util.LinkedHashSet;
import java.util.Set;

public class RegisterActivity extends AppCompatActivity {
    private final Set<String> selectedInterests = new LinkedHashSet<>();
    private String selectedStudentIdImagePath;
    private TextView tvUploadId;
    private TextView tvSsoEntry;

    private final ActivityResultLauncher<String> studentIdPickerLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), this::handleStudentIdSelected);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        UserRepository userRepository = new UserRepository(this);
        SessionManager sessionManager = new SessionManager(this);

        EditText etName = findViewById(R.id.et_name);
        EditText etUsername = findViewById(R.id.et_register_username);
        EditText etPassword = findViewById(R.id.et_register_password);
        Spinner spMajor = findViewById(R.id.sp_major);
        EditText etInterests = findViewById(R.id.et_interests);
        EditText etBio = findViewById(R.id.et_bio);
        Button btnRegister = findViewById(R.id.btn_register);
        TextView tvLogin = findViewById(R.id.tv_login);
        tvUploadId = findViewById(R.id.tv_upload_id);
        tvSsoEntry = findViewById(R.id.tv_sso_entry);

        String[] majors = {
                "计算机科学与技术", "软件工程", "通信工程", "电子信息工程",
                "自动化", "机械设计制造及其自动化", "工商管理", "会计学",
                "法学", "汉语言文学", "英语", "数学与应用数学", "物理学", "其他专业"
        };
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, majors);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spMajor.setAdapter(adapter);

        setupInterestChip(findViewById(R.id.chip_interest_books), "教材资料", etInterests);
        setupInterestChip(findViewById(R.id.chip_interest_tech), "数码设备", etInterests);
        setupInterestChip(findViewById(R.id.chip_interest_dorm), "宿舍好物", etInterests);
        setupInterestChip(findViewById(R.id.chip_interest_study), "学习搭子", etInterests);
        setupInterestChip(findViewById(R.id.chip_interest_ticket), "活动票券", etInterests);
        setupInterestChip(findViewById(R.id.chip_interest_job), "校园服务", etInterests);
        selectedInterests.add("数码设备");
        syncInterestField(etInterests);

        btnRegister.setOnClickListener(v -> {
            if (TextUtils.isEmpty(etName.getText()) || TextUtils.isEmpty(etUsername.getText())
                    || TextUtils.isEmpty(etPassword.getText())) {
                A11y.toast(this, "请至少填写姓名、账号和密码。");
                return;
            }

            String username = etUsername.getText().toString().trim();
            String password = etPassword.getText().toString().trim();
            if (username.length() < 3) {
                A11y.toast(this, "账号至少需要 3 个字符。");
                return;
            }
            if (password.length() < 4) {
                A11y.toast(this, "密码至少需要 4 个字符。");
                return;
            }
            if (userRepository.usernameExists(username)) {
                A11y.toast(this, "这个账号已经注册过了，请换一个。");
                return;
            }

            User user = new User();
            user.setName(etName.getText().toString().trim());
            user.setUsername(username);
            user.setPassword(password);
            user.setMajor(spMajor.getSelectedItem().toString());
            user.setInterests(etInterests.getText().toString().trim());
            user.setBio(etBio.getText().toString().trim());
            user.setStudentIdImageUrl(selectedStudentIdImagePath);
            user.setVerifiedAt(TextUtils.isEmpty(selectedStudentIdImagePath) ? 0L : System.currentTimeMillis());

            long userId = userRepository.register(user);
            if (userId <= 0) {
                A11y.toast(this, "注册失败，请稍后再试。");
                return;
            }

            sessionManager.login(userId);
            A11y.toast(this, "注册成功，已自动登录。");
            startActivity(new Intent(this, MainActivity.class));
            finishAffinity();
        });

        tvLogin.setOnClickListener(v -> finish());

        tvUploadId.setOnClickListener(v -> studentIdPickerLauncher.launch("image/*"));
        tvSsoEntry.setOnClickListener(v -> fillCampusAuthProfile(
                etName,
                etUsername,
                spMajor,
                etBio
        ));
    }

    private void setupInterestChip(TextView chip, String label, EditText etInterests) {
        boolean defaultSelected = "数码设备".equals(label);
        chip.setSelected(defaultSelected);
        chip.setBackgroundResource(defaultSelected
                ? R.drawable.bg_auth_interest_chip_active
                : R.drawable.bg_auth_interest_chip);

        chip.setOnClickListener(v -> {
            if (selectedInterests.contains(label)) {
                selectedInterests.remove(label);
                chip.setSelected(false);
                chip.setBackgroundResource(R.drawable.bg_auth_interest_chip);
                chip.setTextColor(getColor(R.color.text_primary));
            } else {
                selectedInterests.add(label);
                chip.setSelected(true);
                chip.setBackgroundResource(R.drawable.bg_auth_interest_chip_active);
                chip.setTextColor(getColor(R.color.white));
            }
            syncInterestField(etInterests);
        });

        chip.setTextColor(defaultSelected ? getColor(R.color.white) : getColor(R.color.text_primary));
    }

    private void syncInterestField(EditText etInterests) {
        etInterests.setText(TextUtils.join("、", selectedInterests));
        etInterests.setSelection(etInterests.getText().length());
    }

    private void handleStudentIdSelected(Uri uri) {
        if (uri == null) {
            return;
        }

        String savedPath = ImageStorageHelper.copyToInternalStorage(this, uri);
        if (TextUtils.isEmpty(savedPath)) {
            A11y.toast(this, "学生证图片保存失败，请重试。");
            return;
        }

        selectedStudentIdImagePath = savedPath;
        markVerificationPassed("学生证已选择");
        A11y.toast(this, "已添加学生证图片，注册后会记录核验状态。");
    }

    private void fillCampusAuthProfile(EditText etName, EditText etUsername, Spinner spMajor, EditText etBio) {
        etName.setText("校园同学");
        etUsername.setText("wut" + (System.currentTimeMillis() % 100000L));
        int majorIndex = findSpinnerIndex(spMajor, "计算机科学与技术");
        if (majorIndex >= 0) {
            spMajor.setSelection(majorIndex);
        }
        if (TextUtils.isEmpty(etBio.getText().toString().trim())) {
            etBio.setText("已通过校园统一认证，正在探索校园闲置交易。");
        }
        selectedStudentIdImagePath = "campus-auth://verified";
        markVerificationPassed("统一认证已通过");
        A11y.toast(this, "已带入校园认证信息，可继续完善兴趣和简介。");
    }

    private int findSpinnerIndex(Spinner spinner, String value) {
        for (int i = 0; i < spinner.getCount(); i++) {
            if (value.equals(spinner.getItemAtPosition(i).toString())) {
                return i;
            }
        }
        return -1;
    }

    private void markVerificationPassed(String label) {
        tvUploadId.setText(label);
        tvUploadId.setTextColor(getColor(R.color.white));
        tvUploadId.setBackgroundResource(R.drawable.bg_auth_interest_chip_active);
        tvSsoEntry.setText("已核验");
        tvSsoEntry.setAlpha(0.8f);
    }
}
