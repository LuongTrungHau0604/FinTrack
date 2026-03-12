package com.example.myapplication.ui.profile;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.example.myapplication.R;
import com.example.myapplication.data.datasource.FirebaseDAO;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class ChangePasswordDialogFragment extends DialogFragment {

    private static final String TAG = "ChangePasswordDialog";

    private TextInputLayout inputLayoutCurrentPassword;
    private TextInputLayout inputLayoutNewPassword;
    private TextInputLayout inputLayoutConfirmPassword;
    private TextInputEditText editTextCurrentPassword;
    private TextInputEditText editTextNewPassword;
    private TextInputEditText editTextConfirmPassword;

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_change_password, null);

        // Initialize views
        inputLayoutCurrentPassword = view.findViewById(R.id.inputLayoutCurrentPassword);
        inputLayoutNewPassword = view.findViewById(R.id.inputLayoutNewPassword);
        inputLayoutConfirmPassword = view.findViewById(R.id.inputLayoutConfirmPassword);
        editTextCurrentPassword = view.findViewById(R.id.editTextCurrentPassword);
        editTextNewPassword = view.findViewById(R.id.editTextNewPassword);
        editTextConfirmPassword = view.findViewById(R.id.editTextConfirmPassword);

        builder.setView(view)
                .setTitle("Change Password")
                .setPositiveButton("Change", null) // Set null for now to prevent auto-dismiss
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

        AlertDialog dialog = builder.create();

        // Override the positive button click listener to prevent auto-dismiss on error
        dialog.setOnShowListener(dialogInterface -> {
            Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            positiveButton.setOnClickListener(v -> {
                if (validateInputs()) {
                    changePassword();
                }
            });
        });

        return dialog;
    }

    private boolean validateInputs() {
        boolean isValid = true;

        String currentPassword = editTextCurrentPassword.getText().toString().trim();
        String newPassword = editTextNewPassword.getText().toString().trim();
        String confirmPassword = editTextConfirmPassword.getText().toString().trim();

        // Validate current password
        if (TextUtils.isEmpty(currentPassword)) {
            inputLayoutCurrentPassword.setError("Current password is required");
            isValid = false;
        } else {
            inputLayoutCurrentPassword.setError(null);
        }

        // Validate new password
        if (TextUtils.isEmpty(newPassword)) {
            inputLayoutNewPassword.setError("New password is required");
            isValid = false;
        } else if (newPassword.length() < 6) {
            inputLayoutNewPassword.setError("Password must be at least 6 characters");
            isValid = false;
        } else {
            inputLayoutNewPassword.setError(null);
        }

        // Validate confirm password
        if (TextUtils.isEmpty(confirmPassword)) {
            inputLayoutConfirmPassword.setError("Please confirm your new password");
            isValid = false;
        } else if (!confirmPassword.equals(newPassword)) {
            inputLayoutConfirmPassword.setError("Passwords do not match");
            isValid = false;
        } else {
            inputLayoutConfirmPassword.setError(null);
        }

        return isValid;
    }

    private void changePassword() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(getContext(), "User not authenticated", Toast.LENGTH_SHORT).show();
            dismiss();
            return;
        }

        String email = user.getEmail();
        String currentPassword = editTextCurrentPassword.getText().toString().trim();
        String newPassword = editTextNewPassword.getText().toString().trim();

        // Show progress (optional - you can add a progress indicator)

        // First re-authenticate the user
        FirebaseDAO firebaseDAO = FirebaseDAO.getInstance();
        firebaseDAO.reauthenticateUser(email, currentPassword, new FirebaseDAO.OnCompleteListener() {
            @Override
            public void onSuccess() {
                // Authentication successful, now change the password
                user.updatePassword(newPassword)
                        .addOnSuccessListener(aVoid -> {
                            Log.d(TAG, "Password updated successfully");
                            Toast.makeText(getContext(), "Password changed successfully", Toast.LENGTH_SHORT).show();
                            dismiss();
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "Failed to update password", e);
                            Toast.makeText(getContext(), "Failed to update password: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        });
            }

            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Authentication failed", e);
                inputLayoutCurrentPassword.setError("Incorrect password");
            }
        });
    }
}