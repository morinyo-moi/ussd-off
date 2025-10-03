package com.example.inbuiltussd;

import android.content.Intent;
import android.telecom.Call;
import android.telecom.InCallService;
import android.util.Log;

public class USSDService extends InCallService {

    public static final String USSD_BROADCAST = "com.example.inbuiltussd.USSD_BROADCAST";
    public static final String EXTRA_USSD_MSG = "ussd_msg";
    public static final String EXTRA_USSD_TYPE = "ussd_type";

    private static final String TAG = "USSDService";
    private static USSDService instance;

    private Call currentCall;
    private int currentState = 0;
    private String enteredPin = "";
    private String enteredAmount = "";
    private int mainMenuSelection = 0;

    public static USSDService getInstance() {
        return instance;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        Log.d(TAG, "USSDService created");
    }

    @Override
    public void onCallAdded(Call call) {
        super.onCallAdded(call);
        Log.d(TAG, "Call added: " + call);
        currentCall = call;
        currentState = 0;

        // Start LOOP USSD journey
        startLoopUssdJourney();

        call.registerCallback(new Call.Callback() {
            @Override
            public void onStateChanged(Call call, int state) {
                Log.d(TAG, "Call state changed: " + state);
                switch (state) {
                    case Call.STATE_DISCONNECTED:
                        sendUssdBroadcast("Session ended. Thank you for using LOOP!", "RESPONSE");
                        resetSession();
                        currentCall = null;
                        break;
                    case Call.STATE_ACTIVE:
                        Log.d(TAG, "USSD call is active");
                        break;
                }
            }

            @Override
            public void onDetailsChanged(Call call, Call.Details details) {
                super.onDetailsChanged(call, details);
                Log.d(TAG, "Call details changed: " + details);
            }
        });
    }

    private void startLoopUssdJourney() {
        // Initial welcome message
        sendUssdBroadcast("Welcome to the WORLD of LOOP\n\nEnter LOOP USSD service PIN:", "REQUEST");
        currentState = 1; // Waiting for PIN
    }

    public void sendUssdReply(String response) {
        if (currentCall == null) {
            Log.w(TAG, "No active call to send USSD reply");
            sendUssdBroadcast("Error: No active USSD session", "ERROR");
            return;
        }

        Log.d(TAG, "Processing USSD response: " + response + ", State: " + currentState);

        switch (currentState) {
            case 1: // PIN Entry
                handlePinEntry(response);
                break;
            case 2: // Main Menu
                handleMainMenu(response);
                break;
            case 3: // Deposit Menu
                handleDepositMenu(response);
                break;
            case 4: // Amount Entry
                handleAmountEntry(response);
                break;
            case 5: // Confirm PIN
                handleConfirmPin(response);
                break;
            default:
                sendUssdBroadcast("Invalid state. Session will be reset.", "ERROR");
                resetSession();
                break;
        }
    }

    private void handlePinEntry(String pin) {
        if (pin.length() == 4 && pin.matches("\\d+")) {
            enteredPin = pin;
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
            currentState = 2; // Main Menu
        } else {
            sendUssdBroadcast("Invalid PIN format. Please enter 4-digit PIN:", "REQUEST");
        }
    }

    private void handleMainMenu(String selection) {
        try {
            mainMenuSelection = Integer.parseInt(selection);
            switch (mainMenuSelection) {
                case 1: // Deposit
                    showDepositMenu();
                    break;
                case 2: // Send Money
                    sendUssdBroadcast("Send Money\n\nFeature coming soon!\n\n0. Back", "REQUEST");
                    break;
                case 3: // Pay to LOOP III
                    sendUssdBroadcast("Pay to LOOP III\n\nFeature coming soon!\n\n0. Back", "REQUEST");
                    break;
                case 4: // Pay LOOP to M-PESA
                    sendUssdBroadcast("Pay LOOP to M-PESA\n\nFeature coming soon!\n\n0. Back", "REQUEST");
                    break;
                case 5: // Loan & Savings
                    sendUssdBroadcast("Loan & Savings\n\nFeature coming soon!\n\n0. Back", "REQUEST");
                    break;
                case 6: // Account Balance
                    sendUssdBroadcast("Your account balance is: KSh 1,500.00\n\n0. Back", "REQUEST");
                    break;
                case 7: // About LOOP
                    sendUssdBroadcast("LOOP - Mobile Money Service\nVersion 2.1.0\n\n0. Back", "REQUEST");
                    break;
                default:
                    sendUssdBroadcast("Invalid selection. Please choose 1-7:", "REQUEST");
                    break;
            }
        } catch (NumberFormatException e) {
            if ("0".equals(selection)) {
                // Back to main menu
                showMainMenu();
            } else {
                sendUssdBroadcast("Invalid input. Please enter a number 1-7 or 0 to go back:", "REQUEST");
            }
        }
    }

    private void showMainMenu() {
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
        currentState = 2;
    }

    private void showDepositMenu() {
        sendUssdBroadcast(
                "Deposit\n\n" +
                        "1. MPESA to LOOP\n" +
                        "2. Airtel Money to LOOP\n\n" +
                        "0. Back",
                "REQUEST"
        );
        currentState = 3; // Deposit Menu
    }

    private void handleDepositMenu(String selection) {
        if ("0".equals(selection)) {
            showMainMenu();
            return;
        }

        try {
            int depositOption = Integer.parseInt(selection);
            if (depositOption == 1 || depositOption == 2) {
                String provider = depositOption == 1 ? "MPESA" : "Airtel Money";
                sendUssdBroadcast("Deposit from " + provider + "\n\nEnter Amount:", "REQUEST");
                currentState = 4; // Amount Entry
            } else {
                sendUssdBroadcast("Invalid selection. Choose 1 or 2:", "REQUEST");
            }
        } catch (NumberFormatException e) {
            sendUssdBroadcast("Invalid input. Please enter 1 or 2:", "REQUEST");
        }
    }

    private void handleAmountEntry(String amount) {
        if ("0".equals(amount)) {
            showDepositMenu();
            return;
        }

        if (amount.matches("\\d+") && Integer.parseInt(amount) > 0) {
            enteredAmount = amount;
            sendUssdBroadcast("Enter PIN to confirm deposit of KSh " + amount + ":", "REQUEST");
            currentState = 5; // Confirm PIN
        } else {
            sendUssdBroadcast("Invalid amount. Please enter a valid amount:", "REQUEST");
        }
    }

    private void handleConfirmPin(String pin) {
        if ("0".equals(pin)) {
            sendUssdBroadcast("Enter Amount:", "REQUEST");
            currentState = 4;
            return;
        }

        if (pin.equals(enteredPin)) {
            // Successful transaction
            sendUssdBroadcast(
                    "Deposit Successful!\n\n" +
                            "You have deposited KSh " + enteredAmount + " to your LOOP account.\n" +
                            "New balance: KSh " + (1500 + Integer.parseInt(enteredAmount)) + "\n\n" +
                            "You will receive an SMS confirmation shortly.\n\n" +
                            "Thank you for using LOOP!",
                    "RESPONSE"
            );

            resetSession();
        } else {
            sendUssdBroadcast("PIN incorrect. Please enter your PIN:", "REQUEST");
        }
    }

    private void resetSession() {
        currentState = 0;
        enteredPin = "";
        enteredAmount = "";
        mainMenuSelection = 0;
    }

    private void sendUssdBroadcast(String message, String type) {
        Intent intent = new Intent(USSD_BROADCAST);
        intent.putExtra(EXTRA_USSD_MSG, message);
        intent.putExtra(EXTRA_USSD_TYPE, type);

        // For Android 8.0+ we need to set explicit package
        intent.setPackage(getPackageName());

        sendBroadcast(intent);
        Log.d(TAG, "USSD Broadcast sent: " + type + " - " + message.substring(0, Math.min(50, message.length())));
    }

    @Override
    public void onCallRemoved(Call call) {
        super.onCallRemoved(call);
        Log.d(TAG, "Call removed: " + call);
        if (currentCall == call) {
            currentCall = null;
            resetSession();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        instance = null;
        Log.d(TAG, "USSDService destroyed");
    }
}