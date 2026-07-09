package com.example.bankcards.controller;

import com.example.bankcards.dto.request.TransferRequest;
import com.example.bankcards.dto.response.TransferResponse;
import com.example.bankcards.exception.BusinessException;
import com.example.bankcards.exception.ErrorCode;
import com.example.bankcards.exception.GlobalExceptionHandler;
import com.example.bankcards.service.TransferService;
import com.example.bankcards.support.AbstractControllerTest;
import com.example.bankcards.support.ControllerTestSecurityConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = TransferController.class)
@Import({GlobalExceptionHandler.class, ControllerTestSecurityConfig.class})
class TransferControllerTest extends AbstractControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private TransferService transferService;

    @Test
    void transfer_withoutAuth_returnsUnauthorized() throws Exception {
        TransferRequest request = new TransferRequest(1L, 2L, new BigDecimal("10.00"));

        mockMvc.perform(post("/transfers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void transfer_asAdmin_returnsForbidden() throws Exception {
        TransferRequest request = new TransferRequest(1L, 2L, new BigDecimal("10.00"));

        mockMvc.perform(post("/transfers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void transfer_asUser_returnsCreatedTransfer() throws Exception {
        TransferRequest request = new TransferRequest(1L, 2L, new BigDecimal("10.00"));
        TransferResponse response = new TransferResponse(
                5L, 1L, 2L, new BigDecimal("10.00"), Instant.parse("2026-07-09T12:00:00Z"));

        when(transferService.transfer(any(TransferRequest.class))).thenReturn(response);

        mockMvc.perform(post("/transfers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.amount").value(10.00))
                .andExpect(jsonPath("$.fromCardId").value(1))
                .andExpect(jsonPath("$.toCardId").value(2));
    }

    @Test
    @WithMockUser(roles = "USER")
    void transfer_invalidAmount_returnsValidationError() throws Exception {
        TransferRequest request = new TransferRequest(1L, 2L, new BigDecimal("0.00"));

        mockMvc.perform(post("/transfers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void transfer_insufficientBalance_returnsConflictJson() throws Exception {
        TransferRequest request = new TransferRequest(1L, 2L, new BigDecimal("100.00"));

        when(transferService.transfer(any(TransferRequest.class)))
                .thenThrow(new BusinessException(ErrorCode.INSUFFICIENT_BALANCE));

        mockMvc.perform(post("/transfers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INSUFFICIENT_BALANCE"))
                .andExpect(jsonPath("$.status").value(409));
    }
}
