package br.com.fiap.phase4.billing.adapter.in.web;

import br.com.fiap.phase4.billing.adapter.in.web.advice.BillingExceptionHandler;
import br.com.fiap.phase4.billing.adapter.in.web.dto.UpdateEstimateStatusRequest;
import br.com.fiap.phase4.billing.domain.model.Estimate;
import br.com.fiap.phase4.billing.domain.model.EstimateItem;
import br.com.fiap.phase4.billing.domain.model.EstimateItemType;
import br.com.fiap.phase4.billing.domain.model.EstimateStatus;
import br.com.fiap.phase4.billing.domain.port.in.ApproveEstimateUseCase;
import br.com.fiap.phase4.billing.domain.port.in.GetEstimateUseCase;
import br.com.fiap.phase4.commons.domain.exception.ResourceNotFoundException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import br.com.fiap.phase4.commons.web.advice.CommonsExceptionAdvice;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("Estimate Controller Web Unit Tests")
class EstimateControllerTest {

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Mock
    private GetEstimateUseCase getEstimateUseCase;

    @Mock
    private ApproveEstimateUseCase approveEstimateUseCase;

    @InjectMocks
    private EstimateController estimateController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(estimateController)
                .setControllerAdvice(new CommonsExceptionAdvice(), new BillingExceptionHandler())
                .build();
    }

    @Nested
    @DisplayName("GET /api/v1/estimates/work-order/{workOrderId}")
    class GetEstimateTests {

        @Test
        @DisplayName("Should return 200 OK with estimate details when found")
        void shouldReturnEstimateWhenFound() throws Exception {
            UUID workOrderId = UUID.randomUUID();
            EstimateItem item = EstimateItem.of(null, EstimateItemType.SERVICE, "SRV-01", "Brake Pad Change", new BigDecimal("150.00"), 1);
            Estimate estimate = Estimate.create(workOrderId, "52998224725", "customer@fiap.com.br", List.of(item));

            when(getEstimateUseCase.getByWorkOrderId(workOrderId)).thenReturn(estimate);

            mockMvc.perform(get("/api/v1/estimates/work-order/{workOrderId}", workOrderId)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.workOrderId").value(workOrderId.toString()))
                    .andExpect(jsonPath("$.status").value("PENDING_APPROVAL"))
                    .andExpect(jsonPath("$.totalAmount").value(150.00))
                    .andExpect(jsonPath("$.items[0].itemCodeOrSku").value("SRV-01"));

            verify(getEstimateUseCase).getByWorkOrderId(workOrderId);
        }

        @Test
        @DisplayName("Should return 404 Not Found when estimate does not exist")
        void shouldReturn404WhenNotFound() throws Exception {
            UUID workOrderId = UUID.randomUUID();
            when(getEstimateUseCase.getByWorkOrderId(workOrderId))
                    .thenThrow(new ResourceNotFoundException("Estimate", workOrderId));

            mockMvc.perform(get("/api/v1/estimates/work-order/{workOrderId}", workOrderId)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("PATCH /api/v1/estimates/work-order/{workOrderId}")
    class UpdateEstimateStatusTests {

        @Test
        @DisplayName("Should return 200 OK when customer approves the estimate")
        void shouldApproveEstimate() throws Exception {
            UUID workOrderId = UUID.randomUUID();
            Estimate estimate = Estimate.create(workOrderId, "52998224725", "customer@fiap.com.br", List.of());
            estimate.approve();

            when(approveEstimateUseCase.approveByWorkOrderId(eq(workOrderId), any())).thenReturn(estimate);

            UpdateEstimateStatusRequest request = new UpdateEstimateStatusRequest();
            request.setStatus(UpdateEstimateStatusRequest.StatusEnum.APPROVED);

            mockMvc.perform(patch("/api/v1/estimates/work-order/{workOrderId}", workOrderId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("APPROVED"));

            verify(approveEstimateUseCase).approveByWorkOrderId(eq(workOrderId), any());
        }

        @Test
        @DisplayName("Should return 200 OK when customer rejects the estimate")
        void shouldRejectEstimate() throws Exception {
            UUID workOrderId = UUID.randomUUID();
            Estimate estimate = Estimate.create(workOrderId, "52998224725", "customer@fiap.com.br", List.of());
            estimate.reject("Too expensive");

            when(approveEstimateUseCase.rejectByWorkOrderId(eq(workOrderId), eq("Too expensive"), any())).thenReturn(estimate);

            UpdateEstimateStatusRequest request = new UpdateEstimateStatusRequest();
            request.setStatus(UpdateEstimateStatusRequest.StatusEnum.REJECTED);
            request.setReason("Too expensive");

            mockMvc.perform(patch("/api/v1/estimates/work-order/{workOrderId}", workOrderId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("REJECTED"))
                    .andExpect(jsonPath("$.rejectionReason").value("Too expensive"));

            verify(approveEstimateUseCase).rejectByWorkOrderId(eq(workOrderId), eq("Too expensive"), any());
        }
    }
}
