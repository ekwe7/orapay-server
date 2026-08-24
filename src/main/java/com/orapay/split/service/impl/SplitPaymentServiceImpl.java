package com.orapay.split.service.impl;

import com.orapay.split.dto.request.SplitPaymentRequestDto;
import com.orapay.split.dto.response.SplitPaymentResponseDto;
import com.orapay.split.service.SplitPaymentService;
import org.springframework.stereotype.Service;

@Service
public class SplitPaymentServiceImpl implements SplitPaymentService {

    @Override
    public SplitPaymentResponseDto processSplitPayment(SplitPaymentRequestDto requestDto) {
        return null;
    }
}
