package com.example.myapplication.ui.chat;
import com.google.ai.client.generativeai.type.Content; // Add this import
import androidx.core.content.ContextCompat;


import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.LifecycleOwner;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.myapplication.R;
import com.example.myapplication.data.model.ChatMessage;
import com.example.myapplication.databinding.ActivityChatBinding;
import com.google.ai.client.generativeai.GenerativeModel;
import com.google.ai.client.generativeai.java.GenerativeModelFutures;
import com.google.ai.client.generativeai.type.GenerateContentResponse;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.MoreExecutors;

public class ChatActivity extends AppCompatActivity {
    private static final String TAG = "ChatActivity";
    private static final String API_KEY = "AIzaSyCRCfpuCaE4ldZLGZ8mX_rK1W32tXf1QZ8"; // Your API key

    private ActivityChatBinding binding;
    private ChatAdapter adapter;
    private GenerativeModelFutures modelFutures;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityChatBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Setup toolbar
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        binding.toolbar.setNavigationOnClickListener(v -> onBackPressed());

        // Initialize Gemini model using the Java-friendly Futures API
        GenerativeModel model = new GenerativeModel(
                "gemini-1.5-flash",
                API_KEY
        );
        modelFutures = GenerativeModelFutures.from(model);

        // Setup RecyclerView
        adapter = new ChatAdapter();
        binding.recyclerViewChat.setAdapter(adapter);
        binding.recyclerViewChat.setLayoutManager(new LinearLayoutManager(this));

        // Add welcome message
        adapter.addMessage(new ChatMessage("Hi! I'm your financial assistant. How can I help you today?", ChatMessage.TYPE_AI));

        // Setup send button
        binding.buttonSend.setOnClickListener(v -> sendMessage());
    }


    private void sendMessage() {
        String message = binding.editTextMessage.getText().toString().trim();
        if (message.isEmpty()) return;

        // Add user message to chat
        adapter.addMessage(new ChatMessage(message, ChatMessage.TYPE_USER));
        binding.recyclerViewChat.smoothScrollToPosition(adapter.getItemCount() - 1);

        // Clear input
        binding.editTextMessage.setText("");

        // Show loading indicator
        binding.progressBar.setVisibility(View.VISIBLE);

        // Create the Content object from the message string
        Content content = new Content.Builder()
                .addText(message)
                .build();

        // Use Futures API with the Content object
        Futures.addCallback(
                modelFutures.generateContent(content), // Pass the Content object here
                new FutureCallback<GenerateContentResponse>() {
                    @Override
                    public void onSuccess(GenerateContentResponse result) {
                        String responseText = result.getText();
                        runOnUiThread(() -> {
                            if (binding != null) { // Check if binding is still valid
                                binding.progressBar.setVisibility(View.GONE);
                                adapter.addMessage(new ChatMessage(responseText, ChatMessage.TYPE_AI));
                                binding.recyclerViewChat.smoothScrollToPosition(adapter.getItemCount() - 1);
                            }
                        });
                    }

                    @Override
                    public void onFailure(Throwable t) {
                        Log.e(TAG, "Error getting response", t);
                        runOnUiThread(() -> {
                            if (binding != null) { // Check if binding is still valid
                                binding.progressBar.setVisibility(View.GONE);
                                Toast.makeText(ChatActivity.this,
                                        "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                                adapter.addMessage(new ChatMessage("Sorry, I couldn't process your request.",
                                        ChatMessage.TYPE_AI));
                            }
                        });
                    }
                },
                // Use the main thread executor provided by Android for UI updates
                ContextCompat.getMainExecutor(this) // Use ContextCompat.getMainExecutor
        );
    }

// Make sure you have this import as well:

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}