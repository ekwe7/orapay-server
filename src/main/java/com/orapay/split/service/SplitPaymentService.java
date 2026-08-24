package com.orapay.split.service;

import com.orapay.split.dto.request.SplitPaymentRequestDto;
import com.orapay.split.dto.response.SplitPaymentResponseDto;

public interface SplitPaymentService {

    SplitPaymentResponseDto processSplitPayment(SplitPaymentRequestDto requestDto);
}
