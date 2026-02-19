package com.pedro.dev.couponservice.services;

import com.pedro.dev.couponservice.domain.Coupon;
import com.pedro.dev.couponservice.repository.CouponRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

/**
 * Serviço responsável pela listagem paginada de cupons de desconto.
 *
 * <p>Suporta busca opcional por texto, filtrando pelo código ou pela descrição
 * do cupom de forma case-insensitive. Quando nenhum filtro é informado,
 * retorna todos os cupons ativos paginados.</p>
 */
@Service
@RequiredArgsConstructor
public class ListCoupons {

    private final CouponRepository repository;

    /**
     * Retorna uma página de cupons, opcionalmente filtrados por um termo de busca.
     *
     * @param search   termo de busca aplicado ao código e à descrição do cupom; pode ser {@code null} ou vazio
     * @param pageable configuração de paginação e ordenação
     * @return página contendo os cupons que atendem ao critério de busca
     */
    public Page<Coupon> execute(String search, Pageable pageable) {
        if (search != null && !search.isBlank()) {
            return repository.findByCodeContainingIgnoreCaseOrDescriptionContainingIgnoreCase(
                    search, search, pageable
            );
        }
        return repository.findAll(pageable);
    }
}