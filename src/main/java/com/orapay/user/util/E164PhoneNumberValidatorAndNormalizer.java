package com.orapay.user.util;

import com.orapay.common.exception.BusinessRuleException;
import java.util.regex.Pattern;

public class E164PhoneNumberValidatorAndNormalizer {

    private static final Pattern E164_PHONE_NUMBER_REGEX_PATTERN = Pattern.compile("^\\+[1-9]\\d{1,14}$");

    public static String validateAndNormalizeToE164Format(String rawInputPhoneNumberString) {
        if (rawInputPhoneNumberString == null || rawInputPhoneNumberString.isBlank()) {
            throw new BusinessRuleException("Phone number string cannot be null or blank");
        }

        String strippedAndCleanedPhoneNumberString = rawInputPhoneNumberString.replaceAll("[\\s\\-\\(\\)]", "");

        if (!E164_PHONE_NUMBER_REGEX_PATTERN.matcher(strippedAndCleanedPhoneNumberString).matches()) {
            throw new BusinessRuleException(
                String.format("Invalid phone number format [%s]. Phone numbers must adhere to canonical E.164 format (e.g. +14155552671)", rawInputPhoneNumberString)
            );
        }

        return strippedAndCleanedPhoneNumberString;
    }
}
