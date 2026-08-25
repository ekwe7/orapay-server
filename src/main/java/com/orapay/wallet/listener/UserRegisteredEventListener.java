package com.orapay.wallet.listener;

import com.orapay.user.event.UserRegisteredEvent;
import com.orapay.wallet.service.WalletService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserRegisteredEventListener {

    private final WalletService walletService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleUserRegisteredEvent(UserRegisteredEvent event) {
        log.info("Auto-provisioning default wallet for newly registered user ID: [{}]", event.getNewlyRegisteredUserUniqueId());
        try {
            walletService.provisionWalletForUser(
                event.getNewlyRegisteredUserUniqueId(),
                event.getRegisteredUserPhoneNumber(),
                "NGN"
            );
            log.info("Default NGN wallet successfully provisioned for user ID: [{}]", event.getNewlyRegisteredUserUniqueId());
        } catch (Exception ex) {
            log.error("Failed to auto-provision wallet for user ID: [{}]", event.getNewlyRegisteredUserUniqueId(), ex);
        }
    }
}
 