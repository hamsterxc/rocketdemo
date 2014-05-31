package com.example.rocketdemo.util;

import android.text.TextUtils;

public class CardNumberUtils {

    private static final int FORMATTED_GROUP_LENGTH = 4;
    private static final String FORMATTED_GROUPS_DELIMITER = " ";

    private static final int MIN_LENGTH = 16;
    public static final int MAX_LENGTH = 16;
    public static final int MAX_LENGTH_FORMATTED = 19;

    public static final int BIN_MIN_LENGTH = 6;

    public static String getFormattedCardNumber(final String cardNumber) {
        if (TextUtils.isEmpty(cardNumber)) {
            return "";
        }

        final String rawCardNumber = getRawCardNumber(cardNumber);
        final StringBuilder formatted = new StringBuilder();

        final int length = rawCardNumber.length();
        for (int i = 0; i < length; i += 4) {
            final int end = i + FORMATTED_GROUP_LENGTH;
            formatted.append(rawCardNumber.substring(i, Math.min(end, length)));
            if (end < length) {
                formatted.append(FORMATTED_GROUPS_DELIMITER);
            }
        }

        return formatted.toString();
    }

    public static String getRawCardNumber(final String cardNumber) {
        return cardNumber.replace(FORMATTED_GROUPS_DELIMITER, "");
    }

    public static boolean isLuhnValid(final String cardNumber) {
        int sum = 0;
        boolean alternate = false;

        for (int i = cardNumber.length() - 1; i >= 0; i--) {
            int n = Integer.parseInt(cardNumber.substring(i, i + 1));
            if (alternate) {
                n *= 2;
                if (n > 9) {
                    n = (n % 10) + 1;
                }
            }
            sum += n;
            alternate = !alternate;
        }

        return (sum % 10 == 0);
    }

}
