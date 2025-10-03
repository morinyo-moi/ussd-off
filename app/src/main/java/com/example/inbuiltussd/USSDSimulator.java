package com.example.inbuiltussd;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

public class USSDSimulator {

    public static final String USSD_BROADCAST = "com.example.inbuiltussd.USSD_BROADCAST";
    public static final String EXTRA_USSD_MSG = "ussd_msg";
    public static final String EXTRA_USSD_TYPE = "ussd_type";

    private static final String TAG = "USSDSimulator";
    private Context context;

    private int currentState = 0;
    private String enteredPin = "";
    private String enteredAmount = "";
    private String depositProvider = "";

    // Your actual USSD PIN - only this will work
    private static final String CORRECT_PIN = "0202";

    public USSDSimulator(Context context) {
        this.context = context;
    }

    public void startUssdJourney() {
        currentState = 1; // PIN Entry state
        sendUssdBroadcast(
                "Welcome to the WORLD of LOOP\n\n" +
                        "Enter LOOP USSD service PIN:",
                "REQUEST"
        );
    }

    public void processInput(String input) {
        Log.d(TAG, "Processing input: " + input + ", State: " + currentState);

        switch (currentState) {
            case 1: // PIN Entry
                handlePinEntry(input);
                break;
            case 2: // Main Menu
                handleMainMenu(input);
                break;
            case 3: // Deposit Menu
                handleDepositMenu(input);
                break;
            case 4: // Amount Entry
                handleAmountEntry(input);
                break;
            case 5: // Confirm PIN
                handleConfirmPin(input);
                break;
            case 6: // Processing Transaction
                handleTransaction(input);
                break;
            default:
                sendUssdBroadcast("Session error. Please start again.", "ERROR");
                resetSession();
                break;
        }
    }

    private void handlePinEntry(String pin) {
        if (pin.equals(CORRECT_PIN)) {
            enteredPin = pin;
            showMainMenu();
        } else {
            sendUssdBroadcast("Invalid PIN. Please enter correct PIN:", "REQUEST");
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
                        "7. About LOOP\n\n" +
                        "0. Exit",
                "REQUEST"
        );
        currentState = 2; // Main Menu
    }

    private void handleMainMenu(String selection) {
        if ("0".equals(selection)) {
            sendUssdBroadcast("Thank you for using LOOP. Goodbye!", "RESPONSE");
            resetSession();
            return;
        }

        try {
            int menuChoice = Integer.parseInt(selection);
            switch (menuChoice) {
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
                    showAccountBalance();
                    break;
                case 7: // About LOOP
                    sendUssdBroadcast("LOOP - Mobile Money Service\nVersion 2.1.0\n\n0. Back", "REQUEST");
                    break;
                default:
                    sendUssdBroadcast("Invalid selection. Please choose 1-7 or 0 to exit:", "REQUEST");
                    break;
            }
        } catch (NumberFormatException e) {
            if ("0".equals(selection)) {
                sendUssdBroadcast("Thank you for using LOOP. Goodbye!", "RESPONSE");
                resetSession();
            } else {
                sendUssdBroadcast("Invalid input. Please enter a number 1-7 or 0 to exit:", "REQUEST");
            }
        }
    }

    private void showAccountBalance() {
        sendUssdBroadcast(
                "Account Balance: KSh 1,500.00\n\n" +
                        "Available: KSh 1,500.00\n" +
                        "Loaned: KSh 0.00\n\n" +
                        "0. Back",
                "RESPONSE"
        );
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
                depositProvider = depositOption == 1 ? "MPESA" : "Airtel Money";
                sendUssdBroadcast("Deposit from " + depositProvider + "\n\nEnter Amount:", "REQUEST");
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
            sendUssdBroadcast(
                    "Confirm Deposit:\n\n" +
                            "From: " + depositProvider + "\n" +
                            "Amount: KSh " + amount + "\n" +
                            "To: LOOP Account\n\n" +
                            "1. Confirm\n" +
                            "0. Cancel",
                    "REQUEST"
            );
            currentState = 5; // Confirm Deposit
        } else {
            sendUssdBroadcast("Invalid amount. Please enter a valid amount:", "REQUEST");
        }
    }

    private void handleConfirmPin(String input) {
        if ("0".equals(input)) {
            sendUssdBroadcast("Enter Amount:", "REQUEST");
            currentState = 4;
            return;
        }

        if ("1".equals(input)) {
            sendUssdBroadcast(
                    "Processing transaction...\n\n" +
                            "You will receive a prompt on " + depositProvider + " to complete the deposit.",
                    "REQUEST"
            );
            currentState = 6; // Processing
        } else {
            sendUssdBroadcast("Invalid choice. Enter 1 to Confirm or 0 to Cancel:", "REQUEST");
        }
    }

    private void handleTransaction(String input) {
        // Simulate successful transaction
        int newBalance = 1500 + Integer.parseInt(enteredAmount);
        sendUssdBroadcast(
                "✓ Deposit Successful!\n\n" +
                        "Amount: KSh " + enteredAmount + "\n" +
                        "From: " + depositProvider + "\n" +
                        "New Balance: KSh " + newBalance + "\n\n" +
                        "Transaction ID: TXN" + System.currentTimeMillis() + "\n\n" +
                        "You will receive an SMS confirmation.\n\n" +
                        "Thank you for using LOOP!",
                "RESPONSE"
        );
        resetSession();
    }

    private void resetSession() {
        currentState = 0;
        enteredPin = "";
        enteredAmount = "";
        depositProvider = "";
    }

    private void sendUssdBroadcast(String message, String type) {
        try {
            Intent intent = new Intent(USSD_BROADCAST);
            intent.putExtra(EXTRA_USSD_MSG, message);
            intent.putExtra(EXTRA_USSD_TYPE, type);
            LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
            Log.d(TAG, "USSD Broadcast sent: " + type);
        } catch (Exception e) {
            Log.e(TAG, "Error sending broadcast", e);
        }
    }
}