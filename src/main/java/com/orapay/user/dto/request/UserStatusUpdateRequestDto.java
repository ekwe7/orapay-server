package com.orapay.user.dto.request;

import com.orapay.user.model.UserAccountStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserStatusUpdateRequestDto {

    @NotNull(message = "Target user account status is mandatory")
    private UserAccountStatus targetUserAccountStatus;

    private String statusUpdateReasonDescription;
}
