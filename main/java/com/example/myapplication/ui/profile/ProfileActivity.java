package com.example.myapplication.ui.profile;

import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.bumptech.glide.Glide;
import com.example.myapplication.R;
import com.example.myapplication.data.datasource.FirebaseDAO;
import com.example.myapplication.data.model.UserProfile;
import com.example.myapplication.databinding.ActivityProfileBinding;
import com.example.myapplication.ui.auth.LoginActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ProfileActivity extends AppCompatActivity {
    private static final String TAG = "ProfileActivity";

    private ActivityProfileBinding binding;
    private FirebaseDAO firebaseDAO;
    private FirebaseAuth auth;
    private FirebaseUser currentUser;
    private Uri selectedImageUri;

    private final ActivityResultLauncher<String> imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    selectedImageUri = uri;
                    Glide.with(this)
                            .load(uri)
                            .circleCrop()
                            .into(binding.imageViewProfile);
                }
            });


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());



        // Initialize Firebase components
        firebaseDAO = FirebaseDAO.getInstance();
        auth = FirebaseAuth.getInstance();
        currentUser = auth.getCurrentUser();

        if (currentUser == null) {
            Toast.makeText(this, "You must be logged in to access profile", Toast.LENGTH_SHORT).show();
            navigateToLogin();
            return;
        }

        // Setup toolbar
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Profile");
        }
        setupDarkModeToggle();

        setupListeners();
        loadUserProfile();

    }


    // In your ProfileActivity

    private void setupListeners() {
        // Photo change
        binding.textChangePhoto.setOnClickListener(v -> imagePickerLauncher.launch("image/*"));
        binding.imageViewProfile.setOnClickListener(v -> imagePickerLauncher.launch("image/*"));

        // Display name update
        binding.buttonUpdateDisplayName.setOnClickListener(v -> updateDisplayName());

        // Password change
        binding.buttonChangePassword.setOnClickListener(v -> showChangePasswordDialog());

        // Dark mode toggle
        binding.switchDarkMode.setOnCheckedChangeListener((buttonView, isChecked) -> toggleDarkMode(isChecked));

        // Currency selection

        // Notifications toggle
        binding.switchNotifications.setOnCheckedChangeListener((buttonView, isChecked) -> toggleNotifications(isChecked));

        // Account management
        binding.buttonLogout.setOnClickListener(v -> logoutUser());
    }

    private void loadUserProfile() {
        binding.progressBar.setVisibility(View.VISIBLE);

        if (currentUser != null) {
            // Set basic info from Firebase Auth
            binding.editTextDisplayName.setText(currentUser.getDisplayName());
            binding.textViewEmail.setText(currentUser.getEmail());

            // Set account creation date
            if (currentUser.getMetadata() != null) {
                long creationTimestamp = currentUser.getMetadata().getCreationTimestamp();
                SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
                String formattedDate = sdf.format(new Date(creationTimestamp));
                binding.textViewCreationDate.setText("Account created: " + formattedDate);
            }

            // Load profile picture
            if (currentUser.getPhotoUrl() != null) {
                Glide.with(this)
                        .load(currentUser.getPhotoUrl())
                        .circleCrop()
                        .placeholder(R.drawable.ic_person)
                        .into(binding.imageViewProfile);
            }

            // Load user preferences from Firestore
            firebaseDAO.getUserProfile(currentUser.getUid(), new FirebaseDAO.OnProfileLoadListener() {
                @Override
                public void onSuccess(UserProfile profile) {
                    binding.progressBar.setVisibility(View.GONE);
                    if (profile != null) {
                        // Set app preferences if they exist
                        binding.switchDarkMode.setChecked(profile.isDarkModeEnabled());
                        binding.switchNotifications.setChecked(profile.isNotificationsEnabled());
                        binding.buttonSetCurrency.setText(profile.getDefaultCurrency());
                    }
                }

                @Override
                public void onFailure(Exception e) {
                    binding.progressBar.setVisibility(View.GONE);
                    Toast.makeText(ProfileActivity.this,
                            "Failed to load profile: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Error loading profile", e);
                }
            });
        }
    }

    private void updateDisplayName() {
        String newDisplayName = binding.editTextDisplayName.getText().toString().trim();

        if (TextUtils.isEmpty(newDisplayName)) {
            binding.inputLayoutDisplayName.setError("Display name cannot be empty");
            return;
        }

        binding.progressBar.setVisibility(View.VISIBLE);

        // Update display name in Firebase Auth
        UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                .setDisplayName(newDisplayName)
                .build();

        currentUser.updateProfile(profileUpdates)
                .addOnSuccessListener(aVoid -> {
                    binding.progressBar.setVisibility(View.GONE);
                    Toast.makeText(ProfileActivity.this,
                            "Display name updated successfully",
                            Toast.LENGTH_SHORT).show();

                    // Also update in Firestore
                    updateNameInFirestore(newDisplayName);
                })
                .addOnFailureListener(e -> {
                    binding.progressBar.setVisibility(View.GONE);
                    Toast.makeText(ProfileActivity.this,
                            "Failed to update display name: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void setupDarkModeToggle() {
        // Set initial state based on current theme
        boolean isDarkMode = (getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
        binding.switchDarkMode.setChecked(isDarkMode);

        // Set listener for dark mode toggle
        binding.switchDarkMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            firebaseDAO.toggleDarkMode(isChecked);
        });
    }



    private void updateNameInFirestore(String displayName) {
        // Update display name in Firestore user document
        UserProfile partialUpdate = new UserProfile();
        partialUpdate.setUid(currentUser.getUid());
        partialUpdate.setDisplayName(displayName);

        firebaseDAO.updateUserProfileField(partialUpdate, "displayName", new FirebaseDAO.OnProfileUpdateListener() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "Firestore profile name updated");
            }

            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Failed to update name in Firestore", e);
            }
        });
    }

    private void uploadProfilePicture() {
        if (selectedImageUri == null) return;

        binding.progressBar.setVisibility(View.VISIBLE);

        StorageReference storageRef = FirebaseStorage.getInstance().getReference()
                .child("profile_images")
                .child(currentUser.getUid() + ".jpg");

        storageRef.putFile(selectedImageUri)
                .addOnSuccessListener(taskSnapshot -> {
                    storageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                        // Update profile with new image URL
                        UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                .setPhotoUri(uri)
                                .build();

                        currentUser.updateProfile(profileUpdates)
                                .addOnSuccessListener(aVoid -> {
                                    binding.progressBar.setVisibility(View.GONE);
                                    Toast.makeText(ProfileActivity.this,
                                            "Profile picture updated",
                                            Toast.LENGTH_SHORT).show();

                                    // Update photo URL in Firestore
                                    updatePhotoInFirestore(uri.toString());
                                })
                                .addOnFailureListener(e -> {
                                    binding.progressBar.setVisibility(View.GONE);
                                    Toast.makeText(ProfileActivity.this,
                                            "Failed to update profile image: " + e.getMessage(),
                                            Toast.LENGTH_SHORT).show();
                                });
                    });
                })
                .addOnFailureListener(e -> {
                    binding.progressBar.setVisibility(View.GONE);
                    Toast.makeText(ProfileActivity.this,
                            "Failed to upload image: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void updatePhotoInFirestore(String photoUrl) {
        UserProfile partialUpdate = new UserProfile();
        partialUpdate.setUid(currentUser.getUid());
        partialUpdate.setPhotoUrl(photoUrl);

        firebaseDAO.updateUserProfileField(partialUpdate, "photoUrl", null);
    }

    private void showChangePasswordDialog() {
        ChangePasswordDialogFragment dialog = new ChangePasswordDialogFragment();
        dialog.show(getSupportFragmentManager(), "ChangePasswordDialog");
    }

    private void toggleDarkMode(boolean enabled) {
        // Update app theme
        // ThemeUtils.applyTheme(enabled);

        // Save preference to Firestore
        UserProfile partialUpdate = new UserProfile();
        partialUpdate.setUid(currentUser.getUid());
        partialUpdate.setDarkModeEnabled(enabled);

        firebaseDAO.updateUserProfileField(partialUpdate, "darkModeEnabled", null);
    }



    private void toggleNotifications(boolean enabled) {
        // Update notification settings
        // NotificationUtils.setNotificationsEnabled(this, enabled);

        // Save preference to Firestore
        UserProfile partialUpdate = new UserProfile();
        partialUpdate.setUid(currentUser.getUid());
        partialUpdate.setNotificationsEnabled(enabled);

        firebaseDAO.updateUserProfileField(partialUpdate, "notificationsEnabled", null);
    }

    private void logoutUser() {
        auth.signOut();
        navigateToLogin();
    }



    private void navigateToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }


}