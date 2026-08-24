package com.orapay.transfer.service.impl;

import com.orapay.transfer.dto.request.TransferRequestDto;
import com.orapay.transfer.dto.response.TransferResponseDto;
import com.orapay.transfer.service.TransferService;
import org.springframework.stereotype.Service;

@Service
public class TransferServiceImpl implements TransferService {

    @Override
    public TransferResponseDto processTransfer(TransferRequestDto requestDto) {
        return null;
    }
}
