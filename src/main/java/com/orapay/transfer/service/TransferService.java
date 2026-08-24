package com.orapay.transfer.service;

import com.orapay.transfer.dto.request.TransferRequestDto;
import com.orapay.transfer.dto.response.TransferResponseDto;

public interface TransferService {

    TransferResponseDto processTransfer(TransferRequestDto requestDto);
}
