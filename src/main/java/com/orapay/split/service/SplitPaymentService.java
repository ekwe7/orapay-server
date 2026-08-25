package com.orapay.split.service;

import com.orapay.split.dto.request.CreateSplitTemplateRequestDto;
import com.orapay.split.dto.request.MerchantCheckoutRequestDto;
import com.orapay.split.dto.request.SplitPaymentRequestDto;
import com.orapay.split.dto.response.SplitPaymentResponseDto;
import com.orapay.split.dto.response.SplitTemplateResponseDto;

public interface SplitPaymentService {

    SplitPaymentResponseDto processSplitPayment(SplitPaymentRequestDto requestDto);

    SplitTemplateResponseDto createSplitTemplate(CreateSplitTemplateRequestDto requestDto);

    SplitPaymentResponseDto processMerchantCheckout(MerchantCheckoutRequestDto checkoutDto);
}
