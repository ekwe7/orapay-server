package com.orapay.user.util;

import com.orapay.common.exception.BusinessRuleException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class E164PhoneNumberValidatorAndNormalizerTest {

    @Test
    @DisplayName("Should validate and normalize valid phone numbers to canonical E.164 format")
    void shouldValidateAndNormalizeValidPhoneNumbers() {
        assertEquals("+14155552671", E164PhoneNumberValidatorAndNormalizer.validateAndNormalizeToE164Format("+1 415 555 2671"));
        assertEquals("+2348012345678", E164PhoneNumberValidatorAndNormalizer.validateAndNormalizeToE164Format("+234-801-234-5678"));
        assertEquals("+442079460912", E164PhoneNumberValidatorAndNormalizer.validateAndNormalizeToE164Format("+44 (20) 7946 0912"));
    }

    @Test
    @DisplayName("Should throw BusinessRuleException on invalid phone number formats")
    void shouldThrowExceptionOnInvalidPhoneNumbers() {
        assertThrows(BusinessRuleException.class, () -> E164PhoneNumberValidatorAndNormalizer.validateAndNormalizeToE164Format("08012345678"));
        assertThrows(BusinessRuleException.class, () -> E164PhoneNumberValidatorAndNormalizer.validateAndNormalizeToE164Format("invalid-phone"));
        assertThrows(BusinessRuleException.class, () -> E164PhoneNumberValidatorAndNormalizer.validateAndNormalizeToE164Format(""));
        assertThrows(BusinessRuleException.class, () -> E164PhoneNumberValidatorAndNormalizer.validateAndNormalizeToE164Format(null));
    }
}
