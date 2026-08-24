package com.orapay.wallet.util;

public class AccountNumberGenerator {
    /**
         * Derives a 10-digit account number from an E.164 phone
  number.
         * E.g., '+2348012345678' -> takes last 10 digits ->
  '8012345678'
         */
        public static String
  derive10DigitAccountNumberFromPhone(String e164PhoneNumber) {
            if (e164PhoneNumber == null || e164PhoneNumber.
  isBlank()) {
                throw new IllegalArgumentException("Phone number cannot be null or empty");
            }
            String digitsOnly = e164PhoneNumber.replaceAll("\\D",
  "");
            if (digitsOnly.length() < 10) {
                throw new IllegalArgumentException("Phone number must contain at least 10 digits");
            }
            return digitsOnly.substring(digitsOnly.length() - 10);
        }

    
}
