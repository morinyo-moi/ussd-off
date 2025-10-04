package com.example.inbuiltussd;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import java.util.List;

public class USSDAccessibilityService extends AccessibilityService {
    private static final String TAG = "USSD_Accessibility";
    private Handler handler = new Handler();
    private String currentSessionId;
    private String pendingInput;
    private boolean isWaitingForInput = false;
    private boolean isUSSDInProgress = false;
    private boolean hasDialedUSSD = false;

    // Add this to track last processed content and prevent duplicates
    private String lastProcessedText = "";
    private long lastProcessedTime = 0;

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (isUSSDDialog(event) && isUSSDInProgress) {
            Log.d(TAG, "🎯 USSD Dialog Detected");

            handler.postDelayed(() -> {
                processUSSDDialog();
            }, 1000);
        }
    }

    @Override
    public void onInterrupt() {
        Log.w(TAG, "⚠️ Service Interrupted");
        resetSession();
    }

    @Override
    public void onServiceConnected() {
        Log.d(TAG, "✅ USSD Service Connected");
        setupService();
    }

    private void setupService() {
        AccessibilityServiceInfo info = new AccessibilityServiceInfo();
        info.eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED |
                AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED;
        info.packageNames = new String[]{
                "com.android.phone",
                "com.google.android.dialer",
                "com.samsung.android.dialer",
                "com.android.incallui"
        };
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC;
        info.notificationTimeout = 100;
        setServiceInfo(info);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String action = intent.getAction();
            if ("START_USSD".equals(action)) {
                String ussdCode = intent.getStringExtra("ussd_code");
                currentSessionId = intent.getStringExtra("session_id");

                // Reset flags for new session
                isUSSDInProgress = false;
                hasDialedUSSD = false;

                startUSSDProcess(ussdCode);
            } else if ("SEND_INPUT".equals(action)) {
                pendingInput = intent.getStringExtra("user_input");
                currentSessionId = intent.getStringExtra("session_id");
                isWaitingForInput = true;
                processPendingInput();
            }
        }
        return START_STICKY;
    }

    private void startUSSDProcess(String ussdCode) {
        Log.d(TAG, "🎯 Starting USSD: " + ussdCode);
        broadcastToApp("SESSION_STARTED", "Starting USSD session", currentSessionId);

        // Only dial if not already dialed
        if (!hasDialedUSSD) {
            hasDialedUSSD = true;
            handler.postDelayed(() -> {
                dialUSSDCode(ussdCode);
            }, 1000);
        }
    }

    private void dialUSSDCode(String ussdCode) {
        try {
            String encodedHash = Uri.encode("#");
            String ussd = ussdCode.replace("#", encodedHash);

            Intent intent = new Intent(Intent.ACTION_CALL);
            intent.setData(Uri.parse("tel:" + ussd));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);

            Log.d(TAG, "📞 Dialed USSD: " + ussdCode);
            isUSSDInProgress = true;

        } catch (Exception e) {
            Log.e(TAG, "❌ USSD Dial Error: " + e.getMessage());
            broadcastToApp("ERROR", "Failed to dial USSD", currentSessionId);
            resetSession();
        }
    }

    private boolean isUSSDDialog(AccessibilityEvent event) {
        if (event == null) return false;

        String packageName = event.getPackageName() != null ? event.getPackageName().toString() : "";
        String className = event.getClassName() != null ? event.getClassName().toString() : "";

        boolean isUSSD = packageName.contains("com.android.phone") ||
                packageName.contains("dialer") ||
                packageName.contains("incallui");

        Log.d(TAG, "🔍 Checking USSD - Package: " + packageName + ", Class: " + className + ", isUSSD: " + isUSSD);
        return isUSSD;
    }

    private void processUSSDDialog() {
        Log.d(TAG, "🔄 Processing USSD Dialog");

        AccessibilityNodeInfo rootNode = getRootInActiveWindow();
        if (rootNode == null) {
            Log.w(TAG, "⚠️ No root node found");
            return;
        }

        try {
            String ussdText = extractUSSDText(rootNode);
            Log.d(TAG, "📄 USSD Content:\n" + ussdText);

            // Prevent processing the same content multiple times
            if (shouldProcessUSSDText(ussdText)) {
                lastProcessedText = ussdText;
                lastProcessedTime = System.currentTimeMillis();

                if (!ussdText.trim().isEmpty()) {
                    analyzeAndRespond(ussdText, rootNode);
                } else {
                    Log.w(TAG, "⚠️ Empty USSD response");
                }
            } else {
                Log.d(TAG, "⏭️ Skipping duplicate USSD content");
            }

        } catch (Exception e) {
            Log.e(TAG, "❌ Error processing USSD: " + e.getMessage());
        } finally {
            rootNode.recycle();
        }
    }

    private boolean shouldProcessUSSDText(String currentText) {
        // Don't process if it's the same text we just processed
        if (currentText.equals(lastProcessedText)) {
            return false;
        }

        // Don't process if we just processed something very recently (within 2 seconds)
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastProcessedTime < 2000) {
            return false;
        }

        // Don't process empty or very short texts
        if (currentText.trim().isEmpty() || currentText.length() < 5) {
            return false;
        }

        return true;
    }

    private void analyzeAndRespond(String ussdText, AccessibilityNodeInfo rootNode) {
        String cleanText = ussdText.toLowerCase();

        // Check if this is just the initial dialing screen
        if (cleanText.contains("dialing") || cleanText.contains("calling") ||
                cleanText.contains("connecting") || ussdText.contains("*219#")) {
            Log.d(TAG, "📞 Initial dialing screen - skipping processing");
            broadcastToApp("DIALING", "Dialing USSD code...", currentSessionId);
            return;
        }

        if (cleanText.contains("pin") || cleanText.contains("password")) {
            Log.d(TAG, "🔐 PIN Prompt Detected");
            broadcastToApp("PIN_PROMPT", ussdText, currentSessionId);

            // Auto-fill PIN only once
            if (!isWaitingForInput) {
                handler.postDelayed(() -> {
                    if (isUSSDInProgress) {
                        enterTextInBackground("0303", rootNode);
                        broadcastToApp("INPUT_SENT", "Auto-filled PIN", currentSessionId);
                    }
                }, 1500);
            }

        } else if (cleanText.contains("menu") || cleanText.contains("select")) {
            Log.d(TAG, "📋 Menu Detected");
            broadcastToApp("MENU_OPTIONS", ussdText, currentSessionId);

        } else if (cleanText.contains("enter") || cleanText.contains("input")) {
            Log.d(TAG, "⌨️ Input Required");
            broadcastToApp("INPUT_REQUIRED", ussdText, currentSessionId);

        } else if (cleanText.contains("success") || cleanText.contains("completed")) {
            Log.d(TAG, "✅ Success");
            broadcastToApp("SUCCESS", ussdText, currentSessionId);
            resetSession();

        } else if (cleanText.contains("error") || cleanText.contains("invalid")) {
            Log.d(TAG, "❌ Error");
            broadcastToApp("ERROR", ussdText, currentSessionId);
            resetSession();

        } else if (cleanText.contains("welcome")) {
            Log.d(TAG, "👋 Welcome Screen");
            broadcastToApp("WELCOME_SCREEN", ussdText, currentSessionId);

        } else {
            Log.d(TAG, "📝 General Response");
            broadcastToApp("USSD_RESPONSE", ussdText, currentSessionId);
        }
    }

    private void processPendingInput() {
        if (pendingInput != null && isWaitingForInput && isUSSDInProgress) {
            Log.d(TAG, "🎯 Processing pending input: " + pendingInput);

            handler.postDelayed(() -> {
                AccessibilityNodeInfo rootNode = getRootInActiveWindow();
                if (rootNode != null) {
                    enterTextInBackground(pendingInput, rootNode);
                    rootNode.recycle();
                }
            }, 1000);
        }
    }

    private void enterTextInBackground(String input, AccessibilityNodeInfo rootNode) {
        try {
            // Find input field
            AccessibilityNodeInfo inputField = findInputField(rootNode);
            if (inputField != null) {
                Bundle args = new Bundle();
                args.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, input);
                boolean success = inputField.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args);

                if (success) {
                    Log.d(TAG, "✅ Input entered: " + input);

                    // Press OK/Send button
                    handler.postDelayed(() -> {
                        pressOKButton();
                    }, 800);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "❌ Input error: " + e.getMessage());
        }

        isWaitingForInput = false;
        pendingInput = null;
    }

    private void resetSession() {
        isUSSDInProgress = false;
        hasDialedUSSD = false;
        isWaitingForInput = false;
        pendingInput = null;
        lastProcessedText = "";
        lastProcessedTime = 0;
    }

    private AccessibilityNodeInfo findInputField(AccessibilityNodeInfo root) {
        if (root == null) return null;

        // Look for EditText fields
        List<AccessibilityNodeInfo> editTexts = root.findAccessibilityNodeInfosByViewId("com.android.phone:id/input_field");
        if (!editTexts.isEmpty()) return editTexts.get(0);

        // Fallback to any editable field
        for (int i = 0; i < root.getChildCount(); i++) {
            AccessibilityNodeInfo child = root.getChild(i);
            if (child != null && child.isEditable()) {
                return child;
            }
        }
        return null;
    }

    private void pressOKButton() {
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) return;

        try {
            // Try different button IDs and texts
            String[] buttonIds = {"android:id/button1", "com.android.phone:id/positiveButton"};
            String[] buttonTexts = {"ok", "send", "submit", "continue"};

            for (String buttonId : buttonIds) {
                List<AccessibilityNodeInfo> buttons = root.findAccessibilityNodeInfosByViewId(buttonId);
                for (AccessibilityNodeInfo button : buttons) {
                    if (button.isClickable()) {
                        button.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                        Log.d(TAG, "✅ Clicked button: " + buttonId);
                        return;
                    }
                }
            }

            for (String text : buttonTexts) {
                List<AccessibilityNodeInfo> buttons = root.findAccessibilityNodeInfosByText(text);
                for (AccessibilityNodeInfo button : buttons) {
                    if (button.isClickable()) {
                        button.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                        Log.d(TAG, "✅ Clicked button with text: " + text);
                        return;
                    }
                }
            }

        } catch (Exception e) {
            Log.e(TAG, "❌ Button click error: " + e.getMessage());
        } finally {
            root.recycle();
        }
    }

    private String extractUSSDText(AccessibilityNodeInfo node) {
        if (node == null) return "";

        StringBuilder text = new StringBuilder();

        if (node.getText() != null && !node.getText().toString().trim().isEmpty()) {
            text.append(node.getText().toString().trim()).append("\n");
        }

        if (node.getContentDescription() != null && !node.getContentDescription().toString().trim().isEmpty()) {
            text.append(node.getContentDescription().toString().trim()).append("\n");
        }

        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            if (child != null) {
                text.append(extractUSSDText(child));
                child.recycle();
            }
        }

        return text.toString();
    }

    private void broadcastToApp(String type, String message, String sessionId) {
        try {
            Intent intent = new Intent("USSD_BACKGROUND_RESPONSE");
            intent.putExtra("response_type", type);
            intent.putExtra("message", message);
            intent.putExtra("session_id", sessionId);
            LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
            Log.d(TAG, "📨 Broadcast: " + type);
        } catch (Exception e) {
            Log.e(TAG, "❌ Broadcast error: " + e.getMessage());
        }
    }
}