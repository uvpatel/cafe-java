package com.cafepos.promotion.application;

import com.cafepos.promotion.api.PricingPreviewRequest;
import com.cafepos.promotion.api.PricingPreviewResponse;

import java.util.UUID;

public interface PricingPreviewUseCase {

    PricingPreviewResponse previewPricing(UUID cafeId, UUID orderId, PricingPreviewRequest request);
}
