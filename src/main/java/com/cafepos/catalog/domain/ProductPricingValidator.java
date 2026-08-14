package com.cafepos.catalog.domain;

import com.cafepos.shared.money.Money;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Currency;

@Service
public class ProductPricingValidator {

    public void validatePrice(Money price) {
        Product.validateMoneyPrice(price);
    }

    public void validatePriceForCafeCurrency(Money price, String cafeCurrencyCode) {
        validatePrice(price);
        if (cafeCurrencyCode == null || cafeCurrencyCode.isBlank()) {
            throw new IllegalArgumentException("Cafe currency code must not be null or blank");
        }
        Currency expectedCurrency = Currency.getInstance(cafeCurrencyCode.trim().toUpperCase());
        if (!price.currency().equals(expectedCurrency)) {
            throw new IllegalArgumentException("Price currency '" + price.currency().getCurrencyCode() 
                    + "' does not match cafe currency '" + expectedCurrency.getCurrencyCode() + "'");
        }
    }

    public Money calculateDiscountedPrice(Money originalPrice, BigDecimal discountPercentage) {
        validatePrice(originalPrice);
        if (discountPercentage == null || discountPercentage.compareTo(BigDecimal.ZERO) < 0 
                || discountPercentage.compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new IllegalArgumentException("Discount percentage must be between 0 and 100");
        }
        BigDecimal multiplier = BigDecimal.ONE.subtract(discountPercentage.divide(BigDecimal.valueOf(100)));
        return originalPrice.multiply(multiplier);
    }
}
