package com.example.inbuiltussd;


public class USSDCommand {
    private String code;
    private String description;
    private CommandType type;

    public enum CommandType {
        BALANCE_CHECK, SEND_MONEY, BUY_AIRTIME, ACCOUNT_INFO
    }

    public USSDCommand(String code, String description, CommandType type) {
        this.code = code;
        this.description = description;
        this.type = type;
    }

    // Getters and setters
    public String getCode() { return code; }
    public String getDescription() { return description; }
    public CommandType getType() { return type; }
}