package com.cafepos.reporting.application;

public final class FormulaSanitizer {

    private FormulaSanitizer() {
    }

    public static String sanitize(String input) {
        if (input == null) {
            return "";
        }
        String trimmed = input.trim();
        if (trimmed.isEmpty()) {
            return "";
        }
        char firstChar = trimmed.charAt(0);
        if (firstChar == '=' || firstChar == '+' || firstChar == '-' || firstChar == '@' || firstChar == '\t' || firstChar == '\r') {
            return "'" + trimmed;
        }
        return trimmed;
    }
}
