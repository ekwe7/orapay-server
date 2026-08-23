package com.orapay.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequestDto {

    @NotBlank(message = "Email address or phone number is mandatory")
    private String emailOrPhone;

    @NotBlank(message = "Password is mandatory")
    private String password;
}
