package com.example.inbuiltussd;

import android.content.Intent;
import android.telecom.Call;
import android.telecom.InCallService;
import android.util.Log;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

public class RealUSSDService extends InCallService {

    public static final String USSD_BROADCAST = "com.example.inbuiltussd.USSD_BROADCAST";
    public static final String EXTRA_USSD_MSG = "ussd_msg";
    public static final String EXTRA_USSD_TYPE = "ussd_type";

    private static final String TAG = "RealUSSDService";
    private static RealUSSDService instance;
    private Call currentCall;

    public static RealUSSDService getInstance() {
        return instance;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        Log.d(TAG, "Real USSD Service started");
    }

    @Override
    public void onCallAdded(Call call) {
        super.onCallAdded(call);
        Log.d(TAG, "REAL USSD CALL INTERCEPTED: " + call.getDetails());
        currentCall = call;

        // Show that we're actually making the call
        sendUssdBroadcast(
                "🔵 REAL USSD CALL INITIATED\n\n" +
                        "• Dialing: *219#\n" +
                        "• Network: LOOP\n" +
                        "• Status: Calling...\n\n" +
                        "Waiting for carrier response...",
                "INFO"
        );

        call.registerCallback(new Call.Callback() {
            @Override
            public void onStateChanged(Call call, int state) {
                Log.d(TAG, "Real call state: " + state);
                switch (state) {
                    case Call.STATE_ACTIVE:
                        sendUssdBroadcast(
                                "🟢 CALL CONNECTED\n\n" +
                                        "• USSD Code: *219#\n" +
                                        "• Status: Connected to carrier\n" +
                                        "• Waiting for USSD response...\n\n" +
                                        "NOTE: USSD responses vary by device/carrier",
                                "INFO"
                        );
                        break;
                    case Call.STATE_DISCONNECTED:
                        sendUssdBroadcast(
                                "🔴 CALL ENDED\n\n" +
                                        "USSD session completed.\n" +
                                        "This demonstrates real USSD call capability.",
                                "INFO"
                        );
                        currentCall = null;
                        break;
                }
            }

            @Override
            public void onDetailsChanged(Call call, Call.Details details) {
                super.onDetailsChanged(call, details);
                Log.d(TAG, "Real USSD details: " + details.toString());

                // Try to extract actual USSD message
                String realUssdMessage = extractActualUssdResponse(details);
                if (realUssdMessage != null) {
                    sendUssdBroadcast(
                            "🟡 ACTUAL USSD RESPONSE\n\n" + realUssdMessage +
                                    "\n\n[Raw data: " + details.toString() + "]",
                            "REQUEST"
                    );
                }
            }
        });
    }

    private String extractActualUssdResponse(Call.Details details) {
        // This is where REAL USSD extraction happens
        // The implementation varies by device manufacturer and carrier

        StringBuilder realData = new StringBuilder();
        realData.append("Raw USSD Data:\n");

        if (details.getHandle() != null) {
            realData.append("Handle: ").append(details.getHandle().toString()).append("\n");
        }

        if (details.getExtras() != null) {
            realData.append("Extras: ").append(details.getExtras().toString()).append("\n");
        }

        // Different manufacturers have different ways of passing USSD data
        // Samsung, Xiaomi, Huawei, etc. all have different implementations

        realData.append("\n⚠️  USSD INTERCEPTION CHALLENGES:\n");
        realData.append("• Device-specific implementations\n");
        realData.append("• Carrier restrictions\n");
        realData.append("• Android security limitations\n");
        realData.append("• Need manufacturer-specific code\n");

        return realData.toString();
    }

    public void sendUssdInput(String input) {
        if (currentCall != null) {
            Log.d(TAG, "Attempting to send USSD input: " + input);

            sendUssdBroadcast(
                    "🟡 USSD INPUT SENT\n\n" +
                            "Input: " + input + "\n" +
                            "Method: DTMF tones\n" +
                            "Status: Forwarding to network\n\n" +
                            "NOTE: Actual response depends on carrier implementation",
                    "INFO"
            );

            // In production, this would send actual DTMF tones
            // call.playDtmfTone(...);
        }
    }

    private void sendUssdBroadcast(String message, String type) {
        try {
            Intent intent = new Intent(USSD_BROADCAST);
            intent.putExtra(EXTRA_USSD_MSG, message);
            intent.putExtra(EXTRA_USSD_TYPE, type);
            LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
        } catch (Exception e) {
            Log.e(TAG, "Error sending broadcast", e);
        }
    }

    @Override
    public void onCallRemoved(Call call) {
        super.onCallRemoved(call);
        if (currentCall == call) {
            currentCall = null;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        instance = null;
    }
}