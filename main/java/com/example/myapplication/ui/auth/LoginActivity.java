package com.example.myapplication.ui.auth; // Đảm bảo đúng package

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.example.myapplication.ui.home.HomeActivity;
import com.google.android.gms.tasks.OnCompleteListener; // Import OnCompleteListener
import com.google.android.gms.tasks.Task;             // Import Task
import com.google.firebase.auth.AuthResult;         // Import AuthResult
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

// Import ViewBinding class
import com.example.myapplication.databinding.ActivityLoginBinding;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";

    // Khai báo ViewBinding và Firebase Auth
    private ActivityLoginBinding binding;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Inflate layout bằng ViewBinding
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Khởi tạo Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // --- Gắn sự kiện Click ---
        binding.btnLogin.setOnClickListener(v -> loginUser());

        binding.txtForgotPassword.setOnClickListener(v -> {
            // TODO: Xử lý sự kiện quên mật khẩu
            Toast.makeText(LoginActivity.this, "Forgot Password Clicked", Toast.LENGTH_SHORT).show();
            // Ví dụ: Chuyển sang màn hình ForgotPasswordActivity
            // Intent intent = new Intent(LoginActivity.this, ForgotPasswordActivity.class);
            // startActivity(intent);
        });

        binding.btnGoogleSignIn.setOnClickListener(v -> {
            // TODO: Xử lý đăng nhập bằng Google
            Toast.makeText(LoginActivity.this, "Google Sign-In Clicked", Toast.LENGTH_SHORT).show();
            // Cần tích hợp Google Sign-In SDK
        });

        binding.btnAppleSignIn.setOnClickListener(v -> {
            // TODO: Xử lý đăng nhập bằng Apple
            Toast.makeText(LoginActivity.this, "Apple Sign-In Clicked", Toast.LENGTH_SHORT).show();
            // Cần tích hợp Apple Sign-In SDK (phức tạp hơn trên Android)
        });

        binding.txtRegisterPrompt.setOnClickListener(v -> {
            // Chuyển sang màn hình đăng ký
            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class); // Đảm bảo có RegisterActivity
            startActivity(intent);
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Kiểm tra nếu người dùng đã đăng nhập thì chuyển thẳng vào HomeActivity
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            Log.d(TAG, "User already logged in: " + currentUser.getUid());
            goToHomeActivity();
        }
    }

    /**
     * Xử lý logic đăng nhập bằng Email/Password.
     */
    private void loginUser() {
        String email = binding.inputLayoutUsername.getEditText().getText().toString().trim();
        String password = binding.inputLayoutPassword.getEditText().getText().toString().trim();

        // --- Kiểm tra đầu vào ---
        if (TextUtils.isEmpty(email)) {
            binding.inputLayoutUsername.setError("Email is required.");
            return;
        } else {
            binding.inputLayoutUsername.setError(null); // Xóa lỗi nếu đã nhập
        }

        if (TextUtils.isEmpty(password)) {
            binding.inputLayoutPassword.setError("Password is required.");
            return;
        } else {
            binding.inputLayoutPassword.setError(null); // Xóa lỗi nếu đã nhập
        }

        // --- Hiển thị trạng thái đang xử lý ---

        // --- Gọi Firebase Auth để đăng nhập ---
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Đăng nhập thành công
                            Log.d(TAG, "signInWithEmail:success");
                            FirebaseUser user = mAuth.getCurrentUser();
                            Toast.makeText(LoginActivity.this, "Login Successful.", Toast.LENGTH_SHORT).show();
                            goToHomeActivity(); // Chuyển đến màn hình chính
                        } else {
                            // Đăng nhập thất bại
                            Log.w(TAG, "signInWithEmail:failure", task.getException());
                            // Hiển thị lỗi cụ thể hơn nếu có thể
                            String errorMessage = task.getException() != null ? task.getException().getMessage() : "Authentication failed.";
                            Toast.makeText(LoginActivity.this, "Login Failed: " + errorMessage,
                                    Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }

    /**
     * Chuyển sang màn hình HomeActivity.
     */
    private void goToHomeActivity() {
        Intent intent = new Intent(LoginActivity.this, HomeActivity.class);
        // Xóa các Activity trước đó khỏi stack để người dùng không quay lại màn hình Login bằng nút Back
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish(); // Đóng LoginActivity
    }


}