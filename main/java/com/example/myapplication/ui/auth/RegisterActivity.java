package com.example.myapplication.ui.auth;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.example.myapplication.ui.home.HomeActivity;
import com.example.myapplication.data.datasource.FirebaseDAO;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.example.myapplication.databinding.ActivityRegisterBinding;


public class RegisterActivity extends AppCompatActivity {

    private static final String TAG = "RegisterActivity";

    private ActivityRegisterBinding binding;
    private FirebaseAuth mAuth;
    private FirebaseDAO firebaseDAO;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRegisterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mAuth = FirebaseAuth.getInstance();
        firebaseDAO = new FirebaseDAO(); // Khởi tạo DAO cho Firestore

        binding.btnRegister.setOnClickListener(v -> registerUser());
        binding.txtLoginLink.setOnClickListener(v -> goToLoginActivity());
    }

    private void registerUser() {
        String displayName = binding.editTextDisplayName.getText().toString().trim();
        String email = binding.editTextEmailRegister.getText().toString().trim();
        String password = binding.editTextPasswordRegister.getText().toString().trim();
        String confirmPassword = binding.editTextConfirmPassword.getText().toString().trim();

        if (!validateInput(displayName, email, password, confirmPassword)) {
            return;
        }

        showLoading(true);

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "createUserWithEmail:success");
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            updateUserProfileAndCreateDbData(user, displayName, email);
                        } else {
                            Log.e(TAG, "User is null after successful registration.");
                            Toast.makeText(RegisterActivity.this, "Registration succeeded but failed to get user.", Toast.LENGTH_SHORT).show();
                            showLoading(false);
                        }
                    } else {
                        Log.w(TAG, "createUserWithEmail:failure", task.getException());
                        String errorMessage = task.getException() != null ? task.getException().getMessage() : "Registration failed.";
                        Toast.makeText(RegisterActivity.this, "Registration Failed: " + errorMessage, Toast.LENGTH_LONG).show();
                        showLoading(false);
                    }
                });
    }

    private void updateUserProfileAndCreateDbData(FirebaseUser user, String displayName, String email) {
        UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                .setDisplayName(displayName)
                .build();

        user.updateProfile(profileUpdates).addOnCompleteListener(profileTask -> {
            if (profileTask.isSuccessful()) {
                Log.d(TAG, "User profile updated in Auth.");
            } else {
                Log.w(TAG, "Failed to update user profile in Auth.", profileTask.getException());
            }
            // Dù update profile Auth thành công hay không, vẫn tiếp tục tạo data trong DB
            createInitialUserDataInDb(user.getUid(), email, displayName);
        });
    }


    private void createInitialUserDataInDb(String userId, String email, String displayName) {
        firebaseDAO.createInitialUserData(userId, email, displayName, new FirebaseDAO.OnDataInitializedListener() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "Initial data created in Firestore for user: " + userId);
                showLoading(false);
                Toast.makeText(RegisterActivity.this, "Registration Successful!", Toast.LENGTH_SHORT).show();
                goToHomeActivity();
            }

            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Failed to create initial data in Firestore for user: " + userId, e);
                showLoading(false);
                Toast.makeText(RegisterActivity.this, "Registration succeeded but failed to save user data: " + e.getMessage(), Toast.LENGTH_LONG).show();
                // Consider UX: Maybe log out the user or redirect to login?
                // goToLoginActivity();
            }
        });
    }

    private boolean validateInput(String displayName, String email, String password, String confirmPassword) {
        boolean isValid = true;

        if (TextUtils.isEmpty(displayName)) {
            binding.inputLayoutDisplayName.setError("Display Name is required.");
            isValid = false;
        } else {
            binding.inputLayoutDisplayName.setError(null);
        }

        if (TextUtils.isEmpty(email)) {
            binding.inputLayoutEmailRegister.setError("Email is required.");
            isValid = false;
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.inputLayoutEmailRegister.setError("Enter a valid email address.");
            isValid = false;
        } else {
            binding.inputLayoutEmailRegister.setError(null);
        }

        if (TextUtils.isEmpty(password)) {
            binding.inputLayoutPasswordRegister.setError("Password is required.");
            isValid = false;
        } else if (password.length() < 6) {
            binding.inputLayoutPasswordRegister.setError("Password must be at least 6 characters.");
            isValid = false;
        } else {
            binding.inputLayoutPasswordRegister.setError(null);
        }

        if (TextUtils.isEmpty(confirmPassword)) {
            binding.inputLayoutConfirmPassword.setError("Confirm Password is required.");
            isValid = false;
        } else if (!password.equals(confirmPassword)) {
            binding.inputLayoutConfirmPassword.setError("Passwords do not match.");
            isValid = false;
        } else {
            binding.inputLayoutConfirmPassword.setError(null);
        }

        return isValid;
    }

    private void showLoading(boolean isLoading) {
        if(binding != null && binding.progressBarRegister != null && binding.btnRegister != null && binding.txtLoginLink != null){
            binding.progressBarRegister.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            binding.btnRegister.setEnabled(!isLoading);
            binding.txtLoginLink.setEnabled(!isLoading);
            // Disable other inputs if needed
            binding.editTextDisplayName.setEnabled(!isLoading);
            binding.editTextEmailRegister.setEnabled(!isLoading);
            binding.editTextPasswordRegister.setEnabled(!isLoading);
            binding.editTextConfirmPassword.setEnabled(!isLoading);
        } else {
            Log.w(TAG, "Binding or its views are null in showLoading");
        }

    }

    private void goToHomeActivity() {
        Intent intent = new Intent(RegisterActivity.this, HomeActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void goToLoginActivity() {
        Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
        startActivity(intent);
        finish();
    }
}