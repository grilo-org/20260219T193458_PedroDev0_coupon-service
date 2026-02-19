package com.pedro.dev.couponservice.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pedro.dev.couponservice.domain.Coupon;
import com.pedro.dev.couponservice.dto.CouponRequest;
import com.pedro.dev.couponservice.repository.CouponRepository;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.springframework.dao.DataIntegrityViolationException;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CouponIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CouponRepository repository;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("Deve criar um cupom com sucesso sanitizando o código (Create)")
    void shouldCreateCouponWithSanitizedCode() throws Exception {

        CouponRequest request = new CouponRequest(
                "PROMO@#$1",
                "Desconto de Teste",
                new BigDecimal("10.0"),
                LocalDate.now().plusDays(1)
        );

        mockMvc.perform(post("/coupons")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.code").value("PROMO1"))
                .andExpect(jsonPath("$.deleted").doesNotExist());
    }

    @Test
    @DisplayName("Deve retornar erro 400 ao tentar deletar um cupom JÁ DELETADO")
    void shouldReturnBadRequestWhenDeletingAlreadyDeletedCoupon() throws Exception {
        Coupon coupon = createAndSaveCoupon("DEL001", "Para Deletar", new BigDecimal("5.0"), LocalDate.now().plusDays(5));

        mockMvc.perform(delete("/coupons/" + coupon.getId()))
                .andExpect(status().isNoContent());

        boolean existsFisicamente = repository.isAlreadyDeleted(coupon.getId());
        assertTrue(existsFisicamente, "O registro deveria existir no banco marcado como true");

        mockMvc.perform(delete("/coupons/" + coupon.getId()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Coupon is already deleted."));
    }

    @Test
    @DisplayName("Deve retornar 404 ao tentar deletar um cupom que NUNCA existiu")
    void shouldReturnNotFoundForNonExistentCoupon() throws Exception {
        UUID randomId = UUID.randomUUID();

        mockMvc.perform(delete("/coupons/" + randomId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Coupon not found."));
    }

    @Test
    @DisplayName("Deve validar erro de Negócio: Data no Passado")
    void shouldReturnErrorForPastExpirationDate() throws Exception {
        CouponRequest request = new CouponRequest(
                "FAIL01",
                "Data Passada",
                new BigDecimal("10.0"),
                LocalDate.now().minusDays(1) // Ontem
        );

        mockMvc.perform(post("/coupons")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Deve validar erro de Negócio: Código com tamanho errado após limpeza")
    void shouldReturnErrorForInvalidCodeLength() throws Exception {

        CouponRequest request = new CouponRequest(
                "ABC",
                "Código Curto",
                new BigDecimal("10.0"),
                LocalDate.now().plusDays(1)
        );

        mockMvc.perform(post("/coupons")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("O código deve ter exatamente 6 caracteres alfanuméricos após a limpeza."));
    }

    @Test
    @DisplayName("Deve filtrar cupons por código ou descrição")
    void shouldFilterCouponsByName() throws Exception {
        createAndSaveCoupon("NATAL1", "Descrição Padrão", new BigDecimal("10.0"), LocalDate.now().plusDays(10));

        mockMvc.perform(get("/coupons?search=NAT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1) )
                .andExpect(jsonPath("$.content[0].code").value("NATAL1"));

        mockMvc.perform(get("/coupons?search=padrão"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1));
    }

    @Test
    @DisplayName("Deve retornar erro 400 ao criar cupom com desconto abaixo do mínimo (0.1)")
    void shouldReturnBadRequestWhenDiscountBelowMinimum() throws Exception {
        CouponRequest request = new CouponRequest(
                "DISC01",
                "Desconto baixo",
                new BigDecimal("0.1"),
                LocalDate.now().plusDays(1)
        );

        mockMvc.perform(post("/coupons")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("O valor de desconto mínimo é 0.5"));
    }

    @Test
    @DisplayName("Deve criar cupom com sucesso quando desconto é exatamente 0.5 (limite mínimo)")
    void shouldCreateCouponWithMinimumDiscount() throws Exception {
        CouponRequest request = new CouponRequest(
                "MINDC1",
                "Desconto mínimo",
                new BigDecimal("0.5"),
                LocalDate.now().plusDays(1)
        );

        mockMvc.perform(post("/coupons")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.discountValue").value(0.5))
                .andExpect(jsonPath("$.code").value("MINDC1"));
    }

    @Test
    @DisplayName("Deve retornar erro 400 quando discountValue é nulo")
    void shouldReturnBadRequestWhenDiscountValueIsNull() throws Exception {
        CouponRequest request = new CouponRequest(
                "NULLD1",
                "Desconto nulo",
                (BigDecimal) null,
                LocalDate.now().plusDays(1)
        );

        mockMvc.perform(post("/coupons")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Deve retornar erro 400 quando code é nulo")
    void shouldReturnBadRequestWhenCodeIsNull() throws Exception {
        CouponRequest request = new CouponRequest(
                null,
                "Código nulo",
                new BigDecimal("10.0"),
                LocalDate.now().plusDays(1)
        );

        mockMvc.perform(post("/coupons")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Deve criar cupom com sucesso quando data de expiração é hoje (limite)")
    void shouldCreateCouponWithTodayAsExpirationDate() throws Exception {
        CouponRequest request = new CouponRequest(
                "TODAY1",
                "Expira hoje",
                new BigDecimal("10.0"),
                LocalDate.now()
        );

        mockMvc.perform(post("/coupons")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("TODAY1"))
                .andExpect(jsonPath("$.expirationDate").value(LocalDate.now().toString()));
    }

    @Test
    @DisplayName("Deve rejeitar cupom duplicado via constraint única no banco de dados")
    void shouldThrowExceptionForDuplicateCode() {
        repository.saveAndFlush(new Coupon("DUPL01", "Primeiro", new BigDecimal("10.0"), LocalDate.now().plusDays(5)));

        assertThrows(DataIntegrityViolationException.class, () ->
                repository.saveAndFlush(new Coupon("DUPL01", "Segundo", new BigDecimal("5.0"), LocalDate.now().plusDays(5)))
        );
    }

    @Test
    @DisplayName("Deve retornar erro 400 ao criar cupom com descrição vazia")
    void shouldReturnBadRequestWhenDescriptionIsEmpty() throws Exception {
        CouponRequest request = new CouponRequest(
                "EMPTY1",
                "",
                new BigDecimal("10.0"),
                LocalDate.now().plusDays(1)
        );

        mockMvc.perform(post("/coupons")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Deve retornar erro 400 quando código tem mais de 6 caracteres após sanitização")
    void shouldReturnBadRequestWhenCodeTooLongAfterSanitization() throws Exception {
        CouponRequest request = new CouponRequest(
                "ABCDEFG",
                "Código longo",
                new BigDecimal("10.0"),
                LocalDate.now().plusDays(1)
        );

        mockMvc.perform(post("/coupons")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("O código deve ter exatamente 6 caracteres alfanuméricos após a limpeza."));
    }

    @Test
    @DisplayName("Deve garantir que cupons deletados (soft delete) não aparecem na listagem")
    void shouldNotListSoftDeletedCoupons() throws Exception {
        Coupon coupon = createAndSaveCoupon("SOFTD1", "Para Soft Delete", new BigDecimal("10.0"), LocalDate.now().plusDays(10));

        mockMvc.perform(get("/coupons"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].code").value("SOFTD1"));

        mockMvc.perform(delete("/coupons/" + coupon.getId()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/coupons"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(0));
    }

    private Coupon createAndSaveCoupon(String code, String description, BigDecimal discountValue, LocalDate expirationDate) {
        Coupon coupon = new Coupon(code, description, discountValue, expirationDate);
        return repository.save(coupon);
    }
}