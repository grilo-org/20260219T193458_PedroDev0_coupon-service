package com.pedro.dev.couponservice.services;

import com.pedro.dev.couponservice.repository.CouponRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

/**
 * Serviço responsável pela exclusão lógica (soft delete) de cupons de desconto.
 *
 * <p>Implementa uma estratégia de exclusão idempotente com três cenários:</p>
 * <ul>
 *   <li>Cupom ativo encontrado — realiza o soft delete (204)</li>
 *   <li>Cupom já excluído — retorna erro de requisição inválida (400)</li>
 *   <li>Cupom inexistente — retorna erro de não encontrado (404)</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class DeleteCoupon {

    private final CouponRepository repository;

    /**
     * Executa a exclusão lógica de um cupom pelo seu identificador.
     *
     * @param id identificador único do cupom a ser excluído
     * @throws ResponseStatusException com status 400 se o cupom já foi excluído
     * @throws ResponseStatusException com status 404 se o cupom não existe
     */
    @Transactional
    public void execute(UUID id) {
        var coupon = repository.findById(id);

        if (coupon.isPresent()) {
            repository.delete(coupon.get());
            return;
        }

        if (repository.isAlreadyDeleted(id)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Coupon is already deleted.");
        }

        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Coupon not found.");
    }
}