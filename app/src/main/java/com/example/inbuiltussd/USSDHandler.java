package com.example.inbuiltussd;

import android.content.Context;
import android.content.Intent;
import android.telecom.Call;
import android.telecom.InCallService;
import android.util.Log;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

public class USSDHandler extends InCallService {

    public static final String USSD_BROADCAST = "com.example.inbuiltussd.USSD_BROADCAST";
    public static final String EXTRA_USSD_MSG = "ussd_msg";
    public static final String EXTRA_USSD_TYPE = "ussd_type";

    private static final String TAG = "USSDHandler";
    private static USSDHandler instance;
    private Call currentCall;

    public static USSDHandler getInstance() {
        return instance;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        Log.d(TAG, "USSDHandler created");
    }

    @Override
    public void onCallAdded(Call call) {
        super.onCallAdded(call);
        Log.d(TAG, "Real USSD call added: " + call);
        currentCall = call;

        // Listen for USSD responses from the network
        call.registerCallback(new Call.Callback() {
            @Override
            public void onStateChanged(Call call, int state) {
                Log.d(TAG, "Call state changed: " + state);
                switch (state) {
                    case Call.STATE_ACTIVE:
                        // USSD session is active
                        sendUssdBroadcast("Connected to LOOP USSD service\n\nPlease wait for response...", "REQUEST");
                        break;
                    case Call.STATE_DISCONNECTED:
                        sendUssdBroadcast("USSD session ended", "RESPONSE");
                        currentCall = null;
                        break;
                }
            }

            @Override
            public void onDetailsChanged(Call call, Call.Details details) {
                super.onDetailsChanged(call, details);
                Log.d(TAG, "Call details changed: " + details);

                // Handle USSD response
                handleUssdResponse(details);
            }
        });
    }

    private void handleUssdResponse(Call.Details details) {
        // For now, simulate typical LOOP USSD responses
        // In production, replace with actual USSD message extraction

        String simulatedResponse = simulateUssdFlow();
        sendUssdBroadcast(simulatedResponse, "REQUEST");
    }

    private String simulateUssdFlow() {
        // Simulate the LOOP USSD journey
        return "Welcome to the WORLD of LOOP\n\nEnter LOOP USSD service PIN:";
    }

    public void sendUssdInput(String input) {
        if (currentCall != null) {
            Log.d(TAG, "Sending USSD input: " + input);

            // Simulate response based on input
            if (input.equals("0202")) { // Your PIN
                sendUssdBroadcast(
                        "Welcome to the WORLD of LOOP\n\n" +
                                "1. Deposit\n" +
                                "2. Send Money\n" +
                                "3. Pay to LOOP III\n" +
                                "4. Pay LOOP to M-PESA\n" +
                                "5. Loan & Savings\n" +
                                "6. Account Balance\n" +
                                "7. About LOOP",
                        "REQUEST"
                );
            } else {
                sendUssdBroadcast("Invalid PIN. Please try again:", "REQUEST");
            }
        } else {
            Log.w(TAG, "No active call to send USSD input");
            sendUssdBroadcast("Error: No active USSD session", "ERROR");
        }
    }

    private void sendUssdBroadcast(String message, String type) {
        try {
            Intent intent = new Intent(USSD_BROADCAST);
            intent.putExtra(EXTRA_USSD_MSG, message);
            intent.putExtra(EXTRA_USSD_TYPE, type);
            LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
            Log.d(TAG, "USSD Broadcast sent: " + type);
        } catch (Exception e) {
            Log.e(TAG, "Error sending broadcast", e);
        }
    }

    @Override
    public void onCallRemoved(Call call) {
        super.onCallRemoved(call);
        Log.d(TAG, "Call removed");
        if (currentCall == call) {
            currentCall = null;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        instance = null;
        Log.d(TAG, "USSDHandler destroyed");
    }
}