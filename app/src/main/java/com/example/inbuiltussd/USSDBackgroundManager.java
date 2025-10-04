package com.example.inbuiltussd;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import java.util.HashMap;
import java.util.Map;

public class USSDBackgroundManager {
    private static final String TAG = "USSD_Background_Manager";

    private Context context;
    private Handler mainHandler;
    private Map<String, USSDSession> activeSessions;

    public USSDBackgroundManager(Context context) {
        this.context = context;
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.activeSessions = new HashMap<>();
    }

    public void startBackgroundUSSD(String ussdCode, String sessionId) {
        Log.d(TAG, "🎯 Starting background USSD: " + ussdCode + " Session: " + sessionId);

        USSDSession session = new USSDSession(sessionId, ussdCode);
        activeSessions.put(sessionId, session);

        // Notify session start
        broadcastToApp("SESSION_STARTED", "USSD session initiated", sessionId);

        // Start USSD call via Accessibility Service
        initiateUSSDCall(ussdCode, sessionId);
    }

    public void sendUserInput(String input, String sessionId) {
        Log.d(TAG, "📤 Sending user input: " + input + " for session: " + sessionId);

        USSDSession session = activeSessions.get(sessionId);
        if (session != null) {
            session.setLastUserInput(input);
            broadcastToApp("INPUT_SENT", "Input sent: " + input, sessionId);

            // Forward to Accessibility Service
            forwardInputToAccessibilityService(input, sessionId);
        }
    }

    private void initiateUSSDCall(String ussdCode, String sessionId) {
        try {
            String encodedHash = Uri.encode("#");
            String ussdCodeEncoded = ussdCode.replace("#", encodedHash);

            Log.d(TAG, "📞 Dialing USSD in background: " + ussdCode);

            Intent callIntent = new Intent(Intent.ACTION_CALL);
            callIntent.setData(Uri.parse("tel:" + ussdCodeEncoded));
            callIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(callIntent);

            // Schedule automatic dialog hiding
            scheduleAutoHide(sessionId);

        } catch (Exception e) {
            Log.e(TAG, "❌ Background USSD Error: " + e.getMessage());
            broadcastToApp("ERROR", "Failed to start USSD: " + e.getMessage(), sessionId);
        }
    }

    private void scheduleAutoHide(String sessionId) {
        mainHandler.postDelayed(() -> {
            // This will be handled by the enhanced Accessibility Service
            Log.d(TAG, "⏰ Auto-hide scheduled for session: " + sessionId);
        }, 1000);
    }

    private void forwardInputToAccessibilityService(String input, String sessionId) {
        // This method communicates with the Accessibility Service
        Intent serviceIntent = new Intent(context, USSDAccessibilityService.class);
        serviceIntent.setAction("SEND_USER_INPUT");
        serviceIntent.putExtra("user_input", input);
        serviceIntent.putExtra("session_id", sessionId);
        context.startService(serviceIntent);
    }

    public void processUSSDResponse(String response, String type, String sessionId) {
        Log.d(TAG, "🔄 Processing USSD response for session: " + sessionId);

        USSDSession session = activeSessions.get(sessionId);
        if (session != null) {
            session.addToHistory(type + ": " + response);
            broadcastToApp(type, response, sessionId);
        }
    }

    public void endSession(String sessionId, String reason) {
        Log.d(TAG, "🔚 Ending session: " + sessionId + " Reason: " + reason);

        USSDSession session = activeSessions.remove(sessionId);
        if (session != null) {
            broadcastToApp("SESSION_ENDED", reason, sessionId);
        }
    }

    private void broadcastToApp(String type, String message, String sessionId) {
        Intent intent = new Intent("USSD_BACKGROUND_RESPONSE");
        intent.putExtra("response_type", type);
        intent.putExtra("message", message);
        intent.putExtra("session_id", sessionId);
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }

    public void cleanup() {
        for (String sessionId : activeSessions.keySet()) {
            endSession(sessionId, "App closed");
        }
        activeSessions.clear();
    }

    // Session tracking class
    private static class USSDSession {
        private String sessionId;
        private String ussdCode;
        private String lastUserInput;
        private StringBuilder history;
        private long startTime;

        public USSDSession(String sessionId, String ussdCode) {
            this.sessionId = sessionId;
            this.ussdCode = ussdCode;
            this.history = new StringBuilder();
            this.startTime = System.currentTimeMillis();
        }

        public void setLastUserInput(String input) {
            this.lastUserInput = input;
            addToHistory("USER_INPUT: " + input);
        }

        public void addToHistory(String entry) {
            history.append("\n").append(entry);
        }

        public String getHistory() {
            return history.toString();
        }
    }
}