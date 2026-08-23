package com.orapay.auth.service;

import com.orapay.auth.dto.LoginRequestDto;
import com.orapay.auth.dto.RefreshTokenRequestDto;
import com.orapay.auth.dto.TokenResponseDto;
import com.orapay.auth.model.RefreshToken;
import com.orapay.auth.repository.RefreshTokenRepository;
import com.orapay.common.exception.BusinessRuleException;
import com.orapay.user.dto.UserRegistrationRequestDto;
import com.orapay.user.dto.UserResponseDto;
import com.orapay.user.model.KycVerificationTier;
import com.orapay.user.model.User;
import com.orapay.user.model.UserAccountStatus;
import com.orapay.user.repository.UserRepository;
import com.orapay.user.service.UserService;
import com.orapay.user.util.E164PhoneNumberValidatorAndNormalizer;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final UserService userService;

    @Override
    @Transactional
    public TokenResponseDto register(UserRegistrationRequestDto registrationRequestDto) {
        String normalizedE164PhoneNumber = E164PhoneNumberValidatorAndNormalizer
                .validateAndNormalizeToE164Format(registrationRequestDto.getRawInputPhoneNumber());

        if (userRepository.existsByUserEmailAddress(registrationRequestDto.getUserEmailAddress())) {
            throw new BusinessRuleException(
                String.format("Email address [%s] is already registered", registrationRequestDto.getUserEmailAddress())
            );
        }

        if (userRepository.existsByPhoneNumber(normalizedE164PhoneNumber)) {
            throw new BusinessRuleException(
                String.format("Phone number [%s] is already registered", normalizedE164PhoneNumber)
            );
        }

        User newUserEntity = new User();
        newUserEntity.setUserFirstName(registrationRequestDto.getUserFirstName());
        newUserEntity.setUserLastName(registrationRequestDto.getUserLastName());
        newUserEntity.setUserEmailAddress(registrationRequestDto.getUserEmailAddress());
        newUserEntity.setPhoneNumber(normalizedE164PhoneNumber);
        newUserEntity.setPasswordHash(passwordEncoder.encode(registrationRequestDto.getPassword()));
        newUserEntity.setUserAccountStatus(UserAccountStatus.ACTIVE);
        newUserEntity.setKycVerificationTier(KycVerificationTier.TIER_1);

        User savedUser = userRepository.save(newUserEntity);

        return buildTokenResponse(savedUser);
    }

    @Override
    @Transactional
    public TokenResponseDto login(LoginRequestDto loginRequestDto) {
        String input = loginRequestDto.getEmailOrPhone();
        User user = userRepository.findByUserEmailAddress(input)
                .or(() -> userRepository.findByPhoneNumber(input))
                .orElseThrow(() -> new BusinessRuleException("Invalid credentials provided"));

        if (!passwordEncoder.matches(loginRequestDto.getPassword(), user.getPasswordHash())) {
            throw new BusinessRuleException("Invalid credentials provided");
        }

        if (user.getUserAccountStatus() != UserAccountStatus.ACTIVE) {
            throw new BusinessRuleException(
                String.format("Account status is [%s]. Access denied.", user.getUserAccountStatus())
            );
        }

        return buildTokenResponse(user);
    }

    @Override
    @Transactional
    public TokenResponseDto refreshToken(RefreshTokenRequestDto refreshTokenRequestDto) {
        RefreshToken refreshToken = refreshTokenRepository.findByTokenValue(refreshTokenRequestDto.getRefreshToken())
                .orElseThrow(() -> new BusinessRuleException("Invalid or revoked refresh token"));

        if (refreshToken.getExpiryTimestamp().isBefore(Instant.now())) {
            refreshTokenRepository.delete(refreshToken);
            throw new BusinessRuleException("Refresh token has expired. Please log in again.");
        }

        User user = refreshToken.getUser();
        String newAccessToken = jwtService.generateToken(user.getUserUniqueId(), user.getUserEmailAddress());
        UserResponseDto userDto = userService.getUserAccountProfileByUniqueId(user.getUserUniqueId());

        return TokenResponseDto.builder()
                .accessToken(newAccessToken)
                .refreshToken(refreshToken.getTokenValue())
                .tokenType("Bearer")
                .expiresInSeconds(jwtService.getExpirationInSeconds())
                .user(userDto)
                .build();
    }

    @Override
    @Transactional
    public void logout(RefreshTokenRequestDto refreshTokenRequestDto) {
        refreshTokenRepository.deleteByTokenValue(refreshTokenRequestDto.getRefreshToken());
    }

    private TokenResponseDto buildTokenResponse(User user) {
        String accessToken = jwtService.generateToken(user.getUserUniqueId(), user.getUserEmailAddress());

        refreshTokenRepository.deleteByUser(user);
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setTokenValue(UUID.randomUUID().toString());
        refreshToken.setExpiryTimestamp(Instant.now().plus(7, ChronoUnit.DAYS));
        RefreshToken savedRefreshToken = refreshTokenRepository.save(refreshToken);

        UserResponseDto userDto = userService.getUserAccountProfileByUniqueId(user.getUserUniqueId());

        return TokenResponseDto.builder()
                .accessToken(accessToken)
                .refreshToken(savedRefreshToken.getTokenValue())
                .tokenType("Bearer")
                .expiresInSeconds(jwtService.getExpirationInSeconds())
                .user(userDto)
                .build();
    }
}
