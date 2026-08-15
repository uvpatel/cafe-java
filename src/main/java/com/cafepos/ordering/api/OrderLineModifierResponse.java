package com.cafepos.ordering.api;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderLineModifierResponse(
        UUID id,
        UUID modifierGroupId,
        UUID modifierOptionId,
        String optionNameSnapshot,
        BigDecimal priceDeltaSnapshot
) {
}
