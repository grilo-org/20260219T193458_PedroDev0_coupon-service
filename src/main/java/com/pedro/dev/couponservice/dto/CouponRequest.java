package com.pedro.dev.couponservice.dto;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

public record CouponRequest(
        @NotNull String code,
        @NotBlank String description,
        @NotNull BigDecimal discountValue,
        @NotNull @FutureOrPresent LocalDate expirationDate
) {}