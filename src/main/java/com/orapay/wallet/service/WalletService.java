package com.orapay.wallet.service;

import com.orapay.wallet.dto.request.CreateWalletRequestDto;
import com.orapay.wallet.dto.request.FundWalletRequestDto;
import com.orapay.wallet.dto.request.HoldFundsRequestDto;
import com.orapay.wallet.dto.response.WalletResponseDto;

import java.util.UUID;

public interface WalletService {

    WalletResponseDto createWallet(CreateWalletRequestDto requestDto);

    WalletResponseDto getWalletByIdentifier(String identifier);

    void provisionWalletForUser(UUID userId, String phoneNumber, String currencyCode);

    WalletResponseDto fundWallet(UUID walletId, FundWalletRequestDto requestDto);

    WalletResponseDto holdFunds(UUID walletId, HoldFundsRequestDto requestDto);

    WalletResponseDto releaseHold(UUID walletId, HoldFundsRequestDto requestDto);
}
