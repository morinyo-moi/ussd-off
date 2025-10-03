package com.example.inbuiltussd;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import java.util.ArrayList;
import java.util.List;

public class USSDAccessibilityService extends AccessibilityService {
    private static final String TAG = "USSD_Accessibility";
    private Handler handler = new Handler();
    private String lastUSSDText = "";
    private boolean isProcessing = false;
    private long lastEventTime = 0;

    @Override
    public void onServiceConnected() {
        Log.d(TAG, "✅ USSD Stealth Service Connected");
        configureAccessibilityService();
    }

    private void configureAccessibilityService() {
        AccessibilityServiceInfo info = new AccessibilityServiceInfo();
        info.eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED |
                AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED;
        info.packageNames = new String[]{
                "com.android.phone",
                "com.google.android.dialer",
                "com.samsung.android.dialer",
                "com.android.mms",
                "com.android.incallui"
        };
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC;
        info.notificationTimeout = 100;
        info.flags = AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS |
                AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS;
        setServiceInfo(info);
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        // Prevent too frequent processing
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastEventTime < 500) {
            return; // Skip if events are too close
        }
        lastEventTime = currentTime;

        if (isUSSDEvent(event) && !isProcessing) {
            Log.d(TAG, "🎯 USSD Event Detected - Stealth mode");
            isProcessing = true;

            // Gentle approach - don't fight the system
            stealthHideUSSD();
        }
    }

    private boolean isUSSDEvent(AccessibilityEvent event) {
        if (event == null) return false;

        String packageName = event.getPackageName() != null ? event.getPackageName().toString() : "";
        String className = event.getClassName() != null ? event.getClassName().toString() : "";

        return (packageName.contains("phone") ||
                packageName.contains("dialer") ||
                packageName.contains("incallui")) &&
                (className.toLowerCase().contains("dialog") ||
                        event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED);
    }

    private void stealthHideUSSD() {
        Log.d(TAG, "🕵️ Stealth hiding USSD");

        // Step 1: Extract content first
        handler.postDelayed(() -> {
            extractAndProcessUSSD();
        }, 300);

        // Step 2: Gentle hide after processing
        handler.postDelayed(() -> {
            gentleHideDialog();
        }, 800);
    }

    private void extractAndProcessUSSD() {
        Log.d(TAG, "🔄 Extracting USSD content");

        AccessibilityNodeInfo rootNode = getRootInActiveWindow();
        if (rootNode == null) {
            Log.d(TAG, "❌ No root node");
            isProcessing = false;
            return;
        }

        try {
            String ussdText = extractAllText(rootNode);
            if (!ussdText.trim().isEmpty() && !ussdText.equals(lastUSSDText)) {
                lastUSSDText = ussdText;
                Log.d(TAG, "📄 USSD Content: " + ussdText.substring(0, Math.min(50, ussdText.length())));

                processUSSDContent(ussdText);
            }
        } catch (Exception e) {
            Log.e(TAG, "❌ Error extracting USSD: " + e.getMessage());
        } finally {
            if (rootNode != null) {
                rootNode.recycle();
            }
        }
    }

    private void gentleHideDialog() {
        Log.d(TAG, "🎭 Gently hiding dialog");

        // Single back press (less aggressive)
        performGlobalAction(GLOBAL_ACTION_BACK);

        // Bring our app to front gently
        handler.postDelayed(() -> {
            bringAppToFrontGentle();
            isProcessing = false;
        }, 500);
    }

    private void bringAppToFrontGentle() {
        try {
            Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                    Intent.FLAG_ACTIVITY_SINGLE_TOP |
                    Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            startActivity(intent);
            Log.d(TAG, "📱 App brought to front gently");
        } catch (Exception e) {
            Log.e(TAG, "❌ Gentle bring-to-front failed: " + e.getMessage());
        }
    }

    private void processUSSDContent(String ussdText) {
        String cleanText = ussdText.toLowerCase();

        if (cleanText.contains("pin") || cleanText.contains("password") || cleanText.contains("enter")) {
            broadcastToApp("PIN_PROMPT", "Enter PIN", ussdText);
        } else if (cleanText.contains("menu") || cleanText.contains("select") || cleanText.contains("option")) {
            broadcastToApp("MAIN_MENU", "Select Option", ussdText);
        } else if (cleanText.contains("welcome") || cleanText.contains("loop")) {
            broadcastToApp("WELCOME_SCREEN", "Welcome", ussdText);
        } else if (cleanText.contains("success") || cleanText.contains("thank")) {
            broadcastToApp("SUCCESS", "Operation Successful", ussdText);
        } else if (cleanText.contains("error") || cleanText.contains("invalid")) {
            broadcastToApp("ERROR", "Operation Failed", ussdText);
        } else {
            broadcastToApp("GENERAL", "USSD Response", ussdText);
        }
    }

    // Method to handle user input from MainActivity
    public void sendInputToUSSD(String input) {
        Log.d(TAG, "⌨️ Received input from app: " + input);

        handler.post(() -> {
            // Allow USSD to appear briefly for input
            handler.postDelayed(() -> {
                enterTextInUSSD(input);
            }, 400);
        });
    }

    private void enterTextInUSSD(String input) {
        AccessibilityNodeInfo rootNode = getRootInActiveWindow();
        if (rootNode == null) {
            Log.e(TAG, "❌ No USSD dialog for input");
            return;
        }

        try {
            // Find input field
            List<AccessibilityNodeInfo> editTexts = findNodesByClassName(rootNode, "android.widget.EditText");
            if (!editTexts.isEmpty()) {
                AccessibilityNodeInfo inputField = editTexts.get(0);

                // Enter text
                Bundle args = new Bundle();
                args.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, input);
                boolean success = inputField.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args);

                if (success) {
                    Log.d(TAG, "✅ Input entered: " + input);
                    // Auto-submit
                    handler.postDelayed(() -> submitAndHide(input), 600);
                }
            } else {
                Log.e(TAG, "❌ No EditText field found");
            }
        } catch (Exception e) {
            Log.e(TAG, "❌ Input failed: " + e.getMessage());
        } finally {
            if (rootNode != null) {
                rootNode.recycle();
            }
        }
    }

    private void submitAndHide(String input) {
        Log.d(TAG, "🔄 Submitting and hiding");

        AccessibilityNodeInfo rootNode = getRootInActiveWindow();
        if (rootNode == null) return;

        try {
            // Find and click submit button
            String[] buttonTexts = {"ok", "send", "submit", "continue", "next"};
            boolean buttonClicked = false;

            for (String buttonText : buttonTexts) {
                List<AccessibilityNodeInfo> buttons = rootNode.findAccessibilityNodeInfosByText(buttonText);
                for (AccessibilityNodeInfo button : buttons) {
                    if (button.isClickable()) {
                        button.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                        Log.d(TAG, "✅ Button clicked: " + buttonText);
                        buttonClicked = true;
                        break;
                    }
                }
                if (buttonClicked) break;
            }

            // Gentle hide after submission
            handler.postDelayed(() -> {
                performGlobalAction(GLOBAL_ACTION_BACK);
                bringAppToFrontGentle();
            }, 400);

        } catch (Exception e) {
            Log.e(TAG, "❌ Submit failed: " + e.getMessage());
        } finally {
            if (rootNode != null) {
                rootNode.recycle();
            }
        }
    }

    // Helper method to find nodes by class name
    private List<AccessibilityNodeInfo> findNodesByClassName(AccessibilityNodeInfo node, String className) {
        List<AccessibilityNodeInfo> result = new ArrayList<>();
        findNodesByClassNameRecursive(node, className, result);
        return result;
    }

    private void findNodesByClassNameRecursive(AccessibilityNodeInfo node, String className, List<AccessibilityNodeInfo> result) {
        if (node == null) return;

        if (node.getClassName() != null && className.equals(node.getClassName().toString())) {
            result.add(AccessibilityNodeInfo.obtain(node));
        }

        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            if (child != null) {
                findNodesByClassNameRecursive(child, className, result);
                child.recycle();
            }
        }
    }

    private String extractAllText(AccessibilityNodeInfo node) {
        if (node == null) return "";

        StringBuilder text = new StringBuilder();
        if (node.getText() != null && !node.getText().toString().trim().isEmpty()) {
            String nodeText = node.getText().toString().trim();
            text.append(nodeText).append("\n");
        }

        if (node.getContentDescription() != null && !node.getContentDescription().toString().trim().isEmpty()) {
            String desc = node.getContentDescription().toString().trim();
            text.append(desc).append("\n");
        }

        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            if (child != null) {
                text.append(extractAllText(child));
                child.recycle();
            }
        }

        return text.toString();
    }

    private void broadcastToApp(String type, String status, String message) {
        try {
            Intent intent = new Intent("USSD_RESPONSE");
            intent.putExtra("response_type", type);
            intent.putExtra("status", status);
            intent.putExtra("message", message);
            LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
            Log.d(TAG, "📨 Broadcast: " + type);
        } catch (Exception e) {
            Log.e(TAG, "❌ Broadcast failed: " + e.getMessage());
        }
    }

    @Override
    public void onInterrupt() {
        Log.w(TAG, "⚠️ Service interrupted");
        isProcessing = false;
    }
}