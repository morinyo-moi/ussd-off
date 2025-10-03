package com.example.inbuiltussd;


public class USSDResponse {
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

    // Constructors, getters, setters
    public USSDResponse(String rawResponse) {
        this.rawResponse = rawResponse;
        this.cleanResponse = cleanResponse(rawResponse);
        this.type = determineType();
    }

    private String cleanResponse(String raw) {
        return raw.replace("CAPTURED USSD RESPONSE", "")
                .replace("ACCESSIBILITY EVENT", "")
                .replace("Event Type:", "")
                .trim();
    }

    private ResponseType determineType() {
        String text = cleanResponse.toLowerCase();
        if (text.contains("welcome") && text.contains("loop")) return ResponseType.WELCOME;
        if (text.contains("enter pin") || text.contains("pin:")) return ResponseType.PIN_PROMPT;
        if (text.contains("1.") && text.contains("2.")) return ResponseType.MAIN_MENU;
        if (text.contains("balance") || text.contains("ksh")) return ResponseType.BALANCE_INFO;
        if (text.contains("success") || text.contains("completed")) return ResponseType.TRANSACTION_SUCCESS;
        if (text.contains("error") || text.contains("invalid")) return ResponseType.ERROR;
        return ResponseType.UNKNOWN;
    }
}
