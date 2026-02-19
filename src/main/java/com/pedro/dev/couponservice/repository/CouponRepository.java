package com.pedro.dev.couponservice.repository;

import com.pedro.dev.couponservice.domain.Coupon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.UUID;

/**
 * Repositório de acesso a dados para a entidade {@link Coupon}.
 *
 * <p>Estende {@link JpaRepository} e fornece consultas customizadas para
 * verificação de exclusão lógica e busca textual com paginação.</p>
 */
@Repository
public interface CouponRepository extends JpaRepository<Coupon, UUID> {

    /**
     * Verifica se um cupom com o identificador informado já foi excluído logicamente.
     *
     * <p>Consulta nativa que ignora o filtro de soft delete do Hibernate,
     * permitindo distinguir entre um cupom inexistente e um já excluído.</p>
     *
     * @param id identificador único do cupom
     * @return {@code true} se o cupom existe e está marcado como excluído
     */
    @Query(value = "SELECT count(*) > 0 FROM coupon WHERE id = :id AND deleted = true", nativeQuery = true)
    boolean isAlreadyDeleted(UUID id);

    /**
     * Busca cupons cujo código ou descrição contenham o termo informado, ignorando maiúsculas/minúsculas.
     *
     * @param code       termo de busca aplicado ao campo código
     * @param description termo de busca aplicado ao campo descrição
     * @param pageable   configuração de paginação e ordenação
     * @return página de cupons que correspondem ao critério de busca
     */
    Page<Coupon> findByCodeContainingIgnoreCaseOrDescriptionContainingIgnoreCase(
            String code, String description, Pageable pageable
    );
}