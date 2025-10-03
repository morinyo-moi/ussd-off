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
import android.view.View;
import android.view.accessibility.AccessibilityManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "USSD_Demo";
    private static final int REQUEST_CALL_PHONE = 101;
    private static final int REQUEST_ACCESSIBILITY = 102;
    private static final int REQUEST_OVERLAY_PERMISSION = 1001;

    private Button startUssdButton, sendInputButton;
    private TextView statusText, respTextView;
    private EditText userInputEditText;
    private LinearLayout inputContainer;
    private boolean isUSSDActive = false;
    private android.os.Handler handler = new android.os.Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initializeViews();
        setupUSSDReceiver();
        checkAccessibilityStatus();
        checkOverlayPermission();

        Log.d(TAG, "🚀 MainActivity Started - Nuclear USSD Ready");
    }

    private void initializeViews() {
        startUssdButton = findViewById(R.id.startUssdButton);
        statusText = findViewById(R.id.statusText);
        respTextView = findViewById(R.id.resp);
        userInputEditText = findViewById(R.id.userInputEditText);
        sendInputButton = findViewById(R.id.sendInputButton);
        inputContainer = findViewById(R.id.inputContainer);

        startUssdButton.setOnClickListener(v -> startUSSDTransaction());
        sendInputButton.setOnClickListener(v -> sendUserInputToUSSD());

        updateInputControlsVisibility(false);
    }

    private void setupUSSDReceiver() {
        BroadcastReceiver ussdReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if ("USSD_RESPONSE".equals(intent.getAction())) {
                    String type = intent.getStringExtra("response_type");
                    String message = intent.getStringExtra("message");
                    Log.d(TAG, "📨 Received USSD: " + type);
                    handleUSSDResponse(type, message);
                }
            }
        };

        LocalBroadcastManager.getInstance(this)
                .registerReceiver(ussdReceiver, new IntentFilter("USSD_RESPONSE"));
    }

    private void handleUSSDResponse(String type, String message) {
        runOnUiThread(() -> {
            respTextView.setText("📱 " + type + "\n\n" + message);

            switch (type) {
                case "PIN_PROMPT":
                case "USSD_CONTENT":
                    statusText.setText("🔐 PIN Required");
                    respTextView.append("\n\n💡 Enter your PIN below:");
                    showInputField("Enter PIN");
                    isUSSDActive = true;
                    break;

                case "MAIN_MENU":
                    statusText.setText("📋 Main Menu");
                    respTextView.append("\n\n💡 Select option (1, 2, 3, etc.):");
                    showInputField("Enter option number");
                    isUSSDActive = true;
                    break;

                case "WELCOME_SCREEN":
                    statusText.setText("👋 Welcome Screen");
                    respTextView.append("\n\n⏳ Continuing automatically...");
                    isUSSDActive = true;
                    handler.postDelayed(() -> sendUserInputDirectly("1"), 2000);
                    break;

                case "SUCCESS":
                case "SUBMITTED":
                    statusText.setText("✅ Operation Successful");
                    respTextView.append("\n\n🎉 USSD completed successfully!");
                    hideInputField();
                    isUSSDActive = false;
                    break;

                case "ERROR":
                    statusText.setText("❌ Operation Failed");
                    respTextView.append("\n\n🔧 Please try again");
                    hideInputField();
                    isUSSDActive = false;
                    break;

                default:
                    statusText.setText("📱 USSD Response");
                    respTextView.append("\n\n💡 Enter your response:");
                    showInputField("Enter response");
                    isUSSDActive = true;
                    break;
            }
        });
    }

    private void sendUserInputDirectly(String input) {
        Log.d(TAG, "📤 Direct sending input: " + input);
        statusText.setText("⌨️ Sending: " + input);
        respTextView.append("\n\n➡️ Auto-sending: " + input + "\n⏳ Processing...");

        Intent intent = new Intent("SEND_USSD_INPUT");
        intent.putExtra("user_input", input);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private void showInputField(String hint) {
        runOnUiThread(() -> {
            userInputEditText.setHint(hint);
            inputContainer.setVisibility(View.VISIBLE);
            userInputEditText.requestFocus();
        });
    }

    private void hideInputField() {
        runOnUiThread(() -> {
            inputContainer.setVisibility(View.GONE);
            userInputEditText.setText("");
        });
    }

    private void updateInputControlsVisibility(boolean visible) {
        runOnUiThread(() -> {
            inputContainer.setVisibility(visible ? View.VISIBLE : View.GONE);
        });
    }

    private void sendUserInputToUSSD() {
        String userInput = userInputEditText.getText().toString().trim();

        if (userInput.isEmpty()) {
            Toast.makeText(this, "Please enter input", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!isUSSDActive) {
            Toast.makeText(this, "No active USSD session", Toast.LENGTH_SHORT).show();
            return;
        }

        Log.d(TAG, "📤 Sending user input to USSD: " + userInput);
        userInputEditText.setText("");
        statusText.setText("⌨️ Sending: " + userInput);
        respTextView.append("\n\n➡️ You entered: " + userInput + "\n⏳ Processing...");

        Intent intent = new Intent("SEND_USSD_INPUT");
        intent.putExtra("user_input", userInput);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
        hideInputField();
    }

    private void startNuclearOverlay() {
        try {
            // Start overlay service for extended period
            Intent overlayIntent = new Intent(this, OverlayService.class);
            startService(overlayIntent);
            Log.d(TAG, "🛡️ Nuclear overlay activated");

            // Keep overlay running for entire USSD session
            handler.postDelayed(() -> {
                try {
                    // Restart overlay to keep it active
                    startService(new Intent(this, OverlayService.class));
                } catch (Exception e) {
                    Log.e(TAG, "❌ Overlay restart failed: " + e.getMessage());
                }
            }, 10000); // Restart every 10 seconds
        } catch (Exception e) {
            Log.e(TAG, "❌ Nuclear overlay failed: " + e.getMessage());
        }
    }

    private void checkAccessibilityStatus() {
        if (isAccessibilityServiceEnabled()) {
            updateStatus("✅ Nuclear USSD Service Active");
            respTextView.setText("Nuclear USSD service is ready\n\n" +
                    "💣 USSD dialogs will be aggressively hidden\n" +
                    "🛡️ Overlay protection enabled\n" +
                    "💬 All interaction happens in this app\n" +
                    "👤 User won't see ANY USSD screens");
        } else {
            updateStatus("🔧 Enable Nuclear USSD Service");
            respTextView.setText("Please enable accessibility service for nuclear USSD operation");
        }
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

    private void startUSSDTransaction() {
        if (!isAccessibilityServiceEnabled()) {
            requestAccessibilityPermission();
            return;
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE)
                != PackageManager.PERMISSION_GRANTED) {
            requestCallPhonePermission();
        } else {
            initiateUSSDCall();
        }
    }

    private void initiateUSSDCall() {
        try {
            String ussdCode = "*219#";
            String encodedHash = Uri.encode("#");
            String ussdCodeEncoded = ussdCode.replace("#", encodedHash);

            Log.d(TAG, "📞 Dialing USSD with calm approach: " + ussdCode);

            updateStatus("🚀 Starting Calm USSD...");
            respTextView.setText("Initiating USSD: " + ussdCode + "\n\n" +
                    "🕵️ Stealth Mode Activated:\n" +
                    "• USSD will run in background\n" +
                    "• No screen flickering\n" +
                    "• Smooth user experience\n" +
                    "• Your app stays visible");

            isUSSDActive = true;

            // Start brief overlay (3 seconds only)
            startBriefOverlay();

            Intent callIntent = new Intent(Intent.ACTION_CALL);
            callIntent.setData(Uri.parse("tel:" + ussdCodeEncoded));
            startActivity(callIntent);

        } catch (Exception e) {
            Log.e(TAG, "❌ USSD Error: " + e.getMessage());
            updateStatus("❌ Error: " + e.getMessage());
            isUSSDActive = false;
        }
    }

    private void startBriefOverlay() {
        try {
            Intent overlayIntent = new Intent(this, OverlayService.class);
            startService(overlayIntent);
            Log.d(TAG, "🛡️ Brief overlay activated");
        } catch (Exception e) {
            Log.e(TAG, "❌ Overlay failed: " + e.getMessage());
        }
    }

    private void startOverlayService() {
        try {
            Intent intent = new Intent(this, OverlayService.class);
            startService(intent);
            Log.d(TAG, "🛡️ Overlay service started");

            handler.postDelayed(() -> {
                try {
                    stopService(new Intent(this, OverlayService.class));
                    Log.d(TAG, "🛡️ Overlay service stopped");
                } catch (Exception e) {
                    Log.e(TAG, "❌ Error stopping overlay: " + e.getMessage());
                }
            }, 5000);
        } catch (Exception e) {
            Log.e(TAG, "❌ Overlay service failed: " + e.getMessage());
        }
    }

    private void requestAccessibilityPermission() {
        new android.app.AlertDialog.Builder(this)
                .setTitle("Enable Nuclear USSD Service")
                .setMessage("This app needs Accessibility permission to completely hide USSD dialogs")
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

    private void checkOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                new android.app.AlertDialog.Builder(this)
                        .setTitle("Overlay Permission Required")
                        .setMessage("This app needs overlay permission to block USSD dialogs from appearing")
                        .setPositiveButton("Grant Permission", (dialog, which) -> {
                            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                    Uri.parse("package:" + getPackageName()));
                            startActivityForResult(intent, REQUEST_OVERLAY_PERMISSION);
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_ACCESSIBILITY) {
            if (isAccessibilityServiceEnabled()) {
                Toast.makeText(this, "Nuclear USSD service enabled!", Toast.LENGTH_SHORT).show();
                updateStatus("✅ Nuclear USSD Service Active");
            }
        }
        else if (requestCode == REQUEST_OVERLAY_PERMISSION) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (Settings.canDrawOverlays(this)) {
                    Toast.makeText(this, "Overlay permission granted!", Toast.LENGTH_SHORT).show();
                    updateStatus("🛡️ Overlay Protection Active");
                }
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CALL_PHONE && grantResults.length > 0 &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            initiateUSSDCall();
        } else {
            updateStatus("❌ Call permission required");
        }
    }

    private void updateStatus(String message) {
        runOnUiThread(() -> statusText.setText("Status: " + message));
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkAccessibilityStatus();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            stopService(new Intent(this, OverlayService.class));
        } catch (Exception e) {
            Log.e(TAG, "Error stopping overlay: " + e.getMessage());
        }
    }
}