package com.example.bankcards.util;

public final class CardMaskUtil {

    private CardMaskUtil() {
    }

    public static String mask(String lastFour) {
        if (lastFour == null || lastFour.length() < 4) {
            return "****";
        }
        return "**** **** **** " + lastFour;
    }

    public static String extractLastFour(String cardNumber) {
        String digits = normalize(cardNumber);
        if (digits.length() < 4) {
            throw new IllegalArgumentException("Card number must contain at least 4 digits");
        }
        return digits.substring(digits.length() - 4);
    }

    public static String normalize(String cardNumber) {
        return cardNumber.replaceAll("\\s+", "");
    }
}
