package com.example.inbuiltussd;

public class USSDResponseParser {
    private String rawResponse;
    private String cleanResponse;
    private ResponseType type;
    private double balance;
    private String[] menuOptions;
    private boolean requiresInput;
    private String inputPrompt;

    public enum ResponseType {
        WELCOME, PIN_PROMPT, MAIN_MENU, BALANCE_INFO,
        TRANSACTION_SUCCESS, ERROR, UNKNOWN
    }

    // Constructor
    public USSDResponseParser(String rawResponse) {
        this.rawResponse = rawResponse;
        this.cleanResponse = cleanResponse(rawResponse);
        this.type = determineType();
    }

    // Getters and Setters
    public String getRawResponse() {
        return rawResponse;
    }

    public void setRawResponse(String rawResponse) {
        this.rawResponse = rawResponse;
    }

    public String getCleanResponse() {
        return cleanResponse;
    }

    public void setCleanResponse(String cleanResponse) {
        this.cleanResponse = cleanResponse;
    }

    public ResponseType getType() {
        return type;
    }

    public void setType(ResponseType type) {
        this.type = type;
    }

    public double getBalance() {
        return balance;
    }

    public void setBalance(double balance) {
        this.balance = balance;
    }

    public String[] getMenuOptions() {
        return menuOptions;
    }

    public void setMenuOptions(String[] menuOptions) {
        this.menuOptions = menuOptions;
    }

    public boolean isRequiresInput() {
        return requiresInput;
    }

    public void setRequiresInput(boolean requiresInput) {
        this.requiresInput = requiresInput;
    }

    public String getInputPrompt() {
        return inputPrompt;
    }

    public void setInputPrompt(String inputPrompt) {
        this.inputPrompt = inputPrompt;
    }

    private String cleanResponse(String raw) {
        if (raw == null) return "";
        return raw.replace("CAPTURED USSD RESPONSE", "")
                .replace("ACCESSIBILITY EVENT", "")
                .replace("Event Type:", "")
                .replace("Class Name:", "")
                .trim();
    }

    private ResponseType determineType() {
        if (cleanResponse == null) return ResponseType.UNKNOWN;

        String text = cleanResponse.toLowerCase();
        if (text.contains("welcome") && text.contains("loop")) {
            return ResponseType.WELCOME;
        } else if (text.contains("enter pin") || text.contains("pin:")) {
            return ResponseType.PIN_PROMPT;
        } else if (text.contains("1.") && text.contains("2.")) {
            return ResponseType.MAIN_MENU;
        } else if (text.contains("balance") || text.contains("ksh")) {
            return ResponseType.BALANCE_INFO;
        } else if (text.contains("success") || text.contains("completed")) {
            return ResponseType.TRANSACTION_SUCCESS;
        } else if (text.contains("invalid") || text.contains("error") || text.contains("failed")) {
            return ResponseType.ERROR;
        } else {
            return ResponseType.UNKNOWN;
        }
    }
}