package com.orapay.user.service;

import com.orapay.common.event.EventPublisher;
import com.orapay.common.exception.BusinessRuleException;
import com.orapay.user.dto.UserRegistrationRequestDto;
import com.orapay.user.dto.UserResponseDto;
import com.orapay.user.dto.UserStatusUpdateRequestDto;
import com.orapay.user.event.UserRegisteredEvent;
import com.orapay.user.event.UserStatusChangedEvent;
import com.orapay.user.mapper.UserMapper;
import com.orapay.user.model.KycVerificationTier;
import com.orapay.user.model.User;
import com.orapay.user.model.UserAccountStatus;
import com.orapay.user.repository.UserRepository;
import com.orapay.user.util.E164PhoneNumberValidatorAndNormalizer;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepositoryInstance;
    private final UserMapper userMapperInstance;
    private final EventPublisher domainEventPublisherInstance;
    private final PasswordEncoder passwordEncoderInstance;

    @Override
    @Transactional
    public UserResponseDto registerNewUserAccount(UserRegistrationRequestDto registrationRequestDto) {
        String normalizedE164PhoneNumber = E164PhoneNumberValidatorAndNormalizer
                .validateAndNormalizeToE164Format(registrationRequestDto.getRawInputPhoneNumber());

        if (userRepositoryInstance.existsByUserEmailAddress(registrationRequestDto.getUserEmailAddress())) {
            throw new BusinessRuleException(
                String.format("Email address [%s] is already registered", registrationRequestDto.getUserEmailAddress())
            );
        }

        if (userRepositoryInstance.existsByPhoneNumber(normalizedE164PhoneNumber)) {
            throw new BusinessRuleException(
                String.format("Phone number [%s] is already registered", normalizedE164PhoneNumber)
            );
        }

        User newUserEntity = new User();
        newUserEntity.setUserFirstName(registrationRequestDto.getUserFirstName());
        newUserEntity.setUserLastName(registrationRequestDto.getUserLastName());
        newUserEntity.setUserEmailAddress(registrationRequestDto.getUserEmailAddress());
        newUserEntity.setPhoneNumber(normalizedE164PhoneNumber);
        newUserEntity.setPasswordHash(passwordEncoderInstance.encode(registrationRequestDto.getPassword()));
        newUserEntity.setUserAccountStatus(UserAccountStatus.ACTIVE);
        newUserEntity.setKycVerificationTier(KycVerificationTier.TIER_1);

        User savedUserEntity = userRepositoryInstance.save(newUserEntity);

        domainEventPublisherInstance.publishEvent(new UserRegisteredEvent(
                this,
                savedUserEntity.getUserUniqueId(),
                savedUserEntity.getUserEmailAddress(),
                savedUserEntity.getPhoneNumber(),
                savedUserEntity.getKycVerificationTier()
        ));

        return userMapperInstance.mapUserEntityToUserResponseDto(savedUserEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponseDto getUserAccountProfileByUniqueId(UUID targetUserUniqueId) {
        User userEntity = userRepositoryInstance.findById(targetUserUniqueId)
                .orElseThrow(() -> new BusinessRuleException(
                    String.format("User account with unique ID [%s] was not found", targetUserUniqueId)
                ));

        return userMapperInstance.mapUserEntityToUserResponseDto(userEntity);
    }

    @Override
    @Transactional
    public UserResponseDto updateUserAccountStatus(UUID targetUserUniqueId, UserStatusUpdateRequestDto statusUpdateRequestDto) {
        User userEntity = userRepositoryInstance.findById(targetUserUniqueId)
                .orElseThrow(() -> new BusinessRuleException(
                    String.format("User account with unique ID [%s] was not found", targetUserUniqueId)
                ));

        UserAccountStatus previousAccountStatus = userEntity.getUserAccountStatus();
        UserAccountStatus updatedAccountStatus = statusUpdateRequestDto.getTargetUserAccountStatus();

        if (previousAccountStatus == updatedAccountStatus) {
            return userMapperInstance.mapUserEntityToUserResponseDto(userEntity);
        }

        userEntity.setUserAccountStatus(updatedAccountStatus);
        User updatedUserEntity = userRepositoryInstance.save(userEntity);

        domainEventPublisherInstance.publishEvent(new UserStatusChangedEvent(
                this,
                updatedUserEntity.getUserUniqueId(),
                previousAccountStatus,
                updatedAccountStatus,
                statusUpdateRequestDto.getStatusUpdateReasonDescription()
        ));

        return userMapperInstance.mapUserEntityToUserResponseDto(updatedUserEntity);
    }
}
