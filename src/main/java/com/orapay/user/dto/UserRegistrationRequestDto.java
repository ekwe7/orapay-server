package com.orapay.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserRegistrationRequestDto {

    @NotBlank(message = "User first name is mandatory")
    @Size(max = 100, message = "First name must not exceed 100 characters")
    private String userFirstName;

    @NotBlank(message = "User last name is mandatory")
    @Size(max = 100, message = "Last name must not exceed 100 characters")
    private String userLastName;

    @NotBlank(message = "User email address is mandatory")
    @Email(message = "Provided email address format is invalid")
    private String userEmailAddress;

    @NotBlank(message = "Raw input phone number is mandatory")
    private String rawInputPhoneNumber;
}
