package com.pedro.dev.couponservice.services;

import com.pedro.dev.couponservice.domain.Coupon;
import com.pedro.dev.couponservice.dto.CouponRequest;
import com.pedro.dev.couponservice.repository.CouponRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Serviço responsável pela criação de cupons de desconto.
 *
 * <p>Recebe os dados de entrada, instancia a entidade {@link Coupon} — que aplica
 * as regras de negócio no construtor (sanitização do código, validação do desconto
 * e da data de expiração) — e persiste o cupom no banco de dados.</p>
 */
@Service
@RequiredArgsConstructor
public class CreateCoupon {

    private final CouponRepository repository;

    /**
     * Cria e persiste um novo cupom de desconto.
     *
     * @param request dados do cupom a ser criado
     * @return o cupom persistido com o identificador gerado
     * @throws IllegalArgumentException se alguma regra de negócio do domínio for violada
     */
    @Transactional
    public Coupon execute(CouponRequest request) {
        Coupon newCoupon = new Coupon(
                request.code(),
                request.description(),
                request.discountValue(),
                request.expirationDate()
        );

        return repository.save(newCoupon);
    }
}