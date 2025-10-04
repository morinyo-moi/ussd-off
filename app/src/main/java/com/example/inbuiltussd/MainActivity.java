package com.example.inbuiltussd;

import android.Manifest;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import android.view.accessibility.AccessibilityManager;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "USSD_Background";
    private static final int REQUEST_CALL_PHONE = 101;
    private static final int REQUEST_ACCESSIBILITY = 103;

    private Button startUssdButton, sendInputButton;
    private TextView statusText, respTextView;
    private EditText ussdInputText;

    private boolean isUSSDActive = false;
    private String currentSessionId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initializeViews();
        setupUSSDReceiver();
        checkPermissions();

        Log.d(TAG, "🚀 MainActivity Started - USSD Ready");
    }

    private void initializeViews() {
        startUssdButton = findViewById(R.id.startUssdButton);
        sendInputButton = findViewById(R.id.sendInputButton);
        statusText = findViewById(R.id.statusText);
        respTextView = findViewById(R.id.resp);
        ussdInputText = findViewById(R.id.ussdInput);

        startUssdButton.setOnClickListener(v -> startBackgroundUSSD());
        sendInputButton.setOnClickListener(v -> sendUserInput());
    }

    private void setupUSSDReceiver() {
        BroadcastReceiver ussdReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if ("USSD_BACKGROUND_RESPONSE".equals(action)) {
                    String type = intent.getStringExtra("response_type");
                    String message = intent.getStringExtra("message");
                    String sessionId = intent.getStringExtra("session_id");

                    Log.d(TAG, "📨 USSD Response: " + type);
                    handleUSSDResponse(type, message, sessionId);
                } else if ("USSD_SESSION_END".equals(action)) {
                    isUSSDActive = false;
                    updateStatus("USSD Session Ended");
                }
            }
        };

        IntentFilter filter = new IntentFilter();
        filter.addAction("USSD_BACKGROUND_RESPONSE");
        filter.addAction("USSD_SESSION_END");
        LocalBroadcastManager.getInstance(this).registerReceiver(ussdReceiver, filter);
    }

    private void checkPermissions() {
        if (!isAccessibilityServiceEnabled()) {
            requestAccessibilityPermission();
            return;
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE)
                != PackageManager.PERMISSION_GRANTED) {
            requestCallPhonePermission();
        }
    }

    private void startBackgroundUSSD() {
        if (!isAccessibilityServiceEnabled()) {
            requestAccessibilityPermission();
            return;
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE)
                != PackageManager.PERMISSION_GRANTED) {
            requestCallPhonePermission();
            return;
        }

        // Prevent multiple clicks
        if (isUSSDActive) {
            Toast.makeText(this, "USSD session already in progress", Toast.LENGTH_SHORT).show();
            return;
        }

        currentSessionId = "ussd_" + System.currentTimeMillis();
        isUSSDActive = true;

        // Disable button to prevent multiple clicks
        startUssdButton.setEnabled(false);

        String ussdCode = "*219#";

        updateStatus("🚀 Starting USSD...");
        respTextView.setText("Session: " + currentSessionId +
                "\nUSSD: " + ussdCode +
                "\n\n⚡ USSD dialog will pop up...");

        // Start USSD via Intent to service
        Intent serviceIntent = new Intent(this, USSDAccessibilityService.class);
        serviceIntent.setAction("START_USSD");
        serviceIntent.putExtra("ussd_code", ussdCode);
        serviceIntent.putExtra("session_id", currentSessionId);
        startService(serviceIntent);
    }



    private void sendUserInput() {
        if (!isUSSDActive) {
            Toast.makeText(this, "No active USSD session", Toast.LENGTH_SHORT).show();
            return;
        }

        String userInput = ussdInputText.getText().toString().trim();
        if (userInput.isEmpty()) {
            Toast.makeText(this, "Please enter input", Toast.LENGTH_SHORT).show();
            return;
        }

        // Send input to service
        Intent serviceIntent = new Intent(this, USSDAccessibilityService.class);
        serviceIntent.setAction("SEND_INPUT");
        serviceIntent.putExtra("user_input", userInput);
        serviceIntent.putExtra("session_id", currentSessionId);
        startService(serviceIntent);

        ussdInputText.setText("");
        updateStatus("📤 Sent: " + userInput);
        respTextView.append("\n\n→ You: " + userInput);
    }

    private void handleUSSDResponse(String type, String message, String sessionId) {
        if (!sessionId.equals(currentSessionId)) {
            return;
        }

        runOnUiThread(() -> {
            switch (type) {
                case "SESSION_STARTED":
                    updateStatus("🔗 USSD Connected");
                    respTextView.append("\n\n📞 Dialing USSD...");
                    break;

                case "WELCOME_SCREEN":
                    updateStatus("👋 USSD Welcome");
                    respTextView.append("\n\n📋 " + message);
                    break;

                case "PIN_PROMPT":
                    updateStatus("🔐 Enter PIN");
                    respTextView.append("\n\n🔐 " + message);
                    // Auto-fill PIN after delay
                    new android.os.Handler().postDelayed(() -> {
                        if (isUSSDActive) {
                            Intent serviceIntent = new Intent(MainActivity.this, USSDAccessibilityService.class);
                            serviceIntent.setAction("SEND_INPUT");
                            serviceIntent.putExtra("user_input", "0303");
                            serviceIntent.putExtra("session_id", currentSessionId);
                            startService(serviceIntent);
                            respTextView.append("\n\n→ Auto-filled: 0303");
                        }
                    }, 1500);
                    break;

                case "MENU_OPTIONS":
                    updateStatus("📋 Menu Options");
                    respTextView.append("\n\n📋 " + message);
                    break;

                case "INPUT_REQUIRED":
                    updateStatus("⌨️ Input Required");
                    respTextView.append("\n\n❓ " + message);
                    break;

                case "SUCCESS":
                    updateStatus("✅ Success");
                    respTextView.append("\n\n✅ " + message);
                    endUSSDsession();
                    break;

                case "ERROR":
                    updateStatus("❌ Error");
                    respTextView.append("\n\n❌ " + message);
                    endUSSDsession();
                    break;

                case "USSD_RESPONSE":
                    updateStatus("📨 Response");
                    respTextView.append("\n\n📨 " + message);
                    break;
            }
        });
    }

    private void endUSSDsession() {
        isUSSDActive = false;
        // Re-enable button
        startUssdButton.setEnabled(true);
    }

    private void requestAccessibilityPermission() {
        new android.app.AlertDialog.Builder(this)
                .setTitle("Enable USSD Service")
                .setMessage("Please enable accessibility service for USSD operations")
                .setPositiveButton("Enable", (dialog, which) -> {
                    Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
                    startActivityForResult(intent, REQUEST_ACCESSIBILITY);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void requestCallPhonePermission() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.CALL_PHONE},
                REQUEST_CALL_PHONE);
    }

    private boolean isAccessibilityServiceEnabled() {
        AccessibilityManager am = (AccessibilityManager) getSystemService(ACCESSIBILITY_SERVICE);
        if (am == null) return false;

        List<AccessibilityServiceInfo> enabledServices =
                am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK);

        for (AccessibilityServiceInfo service : enabledServices) {
            if (service.getId().contains(getPackageName())) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_ACCESSIBILITY) {
            if (isAccessibilityServiceEnabled()) {
                Toast.makeText(this, "USSD Service Enabled!", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CALL_PHONE && grantResults.length > 0 &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Call Permission Granted!", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateStatus(String message) {
        runOnUiThread(() -> statusText.setText("Status: " + message));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        endUSSDsession();
    }
}