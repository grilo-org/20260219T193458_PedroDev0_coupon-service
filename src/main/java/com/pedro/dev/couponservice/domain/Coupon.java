package com.pedro.dev.couponservice.domain;

import jakarta.persistence.*;
import lombok.Getter;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "coupon")
@Getter
@SQLDelete(sql = "UPDATE coupon SET deleted = true WHERE id = ?")
@SQLRestriction("deleted = false")
public class Coupon {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 6)
    private String code;

    @Column(nullable = false)
    private String description;

    @Column(nullable = false)
    private BigDecimal discountValue;

    @Column(nullable = false)
    private LocalDate expirationDate;

    @Column(nullable = false)
    private boolean deleted = false;

    protected Coupon() {}

    public Coupon(String code, String description, BigDecimal discountValue, LocalDate expirationDate) {
        this.code = validateAndSanitizeCode(code);
        this.description = description;
        this.discountValue = validateDiscount(discountValue);
        this.expirationDate = validateExpiration(expirationDate);
    }


    private String validateAndSanitizeCode(String rawCode) {
        if (rawCode == null || rawCode.isBlank()) {
            throw new IllegalArgumentException("O código do cupom não pode ser vazio.");
        }

        String cleanCode = rawCode.replaceAll("[^a-zA-Z0-9]", "").toUpperCase();

        if (cleanCode.length() != 6) {
            throw new IllegalArgumentException("O código deve ter exatamente 6 caracteres alfanuméricos após a limpeza.");
        }
        return cleanCode;
    }

    private BigDecimal validateDiscount(BigDecimal value) {
        if (value == null || value.compareTo(new BigDecimal("0.5")) < 0) {
            throw new IllegalArgumentException("O valor de desconto mínimo é 0.5");
        }
        return value;
    }

    private LocalDate validateExpiration(LocalDate date) {
        if (date == null) {
            throw new IllegalArgumentException("A data de expiração é obrigatória.");
        }
        if (date.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("A data de expiração não pode ser no passado.");
        }
        return date;
    }
}