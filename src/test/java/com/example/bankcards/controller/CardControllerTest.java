package com.example.bankcards.controller;

import com.example.bankcards.dto.request.CreateCardRequest;
import com.example.bankcards.dto.response.CardResponse;
import com.example.bankcards.dto.response.PageResponse;
import com.example.bankcards.entity.enums.CardStatus;
import com.example.bankcards.exception.BusinessException;
import com.example.bankcards.exception.ErrorCode;
import com.example.bankcards.exception.GlobalExceptionHandler;
import com.example.bankcards.service.CardService;
import com.example.bankcards.support.AbstractControllerTest;
import com.example.bankcards.support.ControllerTestSecurityConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = CardController.class)
@Import({GlobalExceptionHandler.class, ControllerTestSecurityConfig.class})
class CardControllerTest extends AbstractControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CardService cardService;

    @Test
    @WithMockUser(roles = "USER")
    void createCard_asUser_returnsForbidden() throws Exception {
        CreateCardRequest request = new CreateCardRequest(
                "4111111111111111",
                LocalDate.now().plusYears(1),
                new BigDecimal("100.00"),
                "john"
        );

        mockMvc.perform(post("/admin/cards")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createCard_asAdmin_returnsCreatedCard() throws Exception {
        CreateCardRequest request = new CreateCardRequest(
                "4111111111111111",
                LocalDate.now().plusYears(1),
                new BigDecimal("100.00"),
                "john"
        );
        CardResponse response = new CardResponse(
                1L, "**** **** **** 1111", "john",
                request.expiryDate(), CardStatus.ACTIVE, request.balance(),
                false, Instant.parse("2026-07-09T12:00:00Z"));

        when(cardService.create(any(CreateCardRequest.class))).thenReturn(response);

        mockMvc.perform(post("/admin/cards")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.maskedNumber").value("**** **** **** 1111"))
                .andExpect(jsonPath("$.owner").value("john"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void getMyCards_asUser_returnsOk() throws Exception {
        when(cardService.getMyCards(isNull(), isNull(), any(Pageable.class)))
                .thenReturn(new PageResponse<>(List.of(), 0, 20, 0, 0, true, true));

        mockMvc.perform(get("/cards"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getMyCards_asAdmin_returnsForbidden() throws Exception {
        mockMvc.perform(get("/cards"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void getMyCard_asUser_returnsCard() throws Exception {
        CardResponse response = new CardResponse(
                1L, "**** **** **** 1234", "john",
                LocalDate.now().plusYears(1), CardStatus.ACTIVE, new BigDecimal("50.00"),
                false, Instant.parse("2026-07-09T12:00:00Z"));

        when(cardService.getMyCard(1L)).thenReturn(response);

        mockMvc.perform(get("/cards/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.maskedNumber").value("**** **** **** 1234"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void getMyCard_notFound_returnsNotFoundJson() throws Exception {
        when(cardService.getMyCard(99L))
                .thenThrow(new BusinessException(ErrorCode.NOT_FOUND, "Card not found: 99"));

        mockMvc.perform(get("/cards/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void delete_asAdmin_returnsNoContent() throws Exception {
        mockMvc.perform(delete("/admin/cards/1"))
                .andExpect(status().isNoContent());

        verify(cardService).delete(1L);
    }
}
