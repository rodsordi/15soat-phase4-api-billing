package br.com.fiap.phase4.billing.domain.usecase;

import br.com.fiap.phase4.billing.domain.model.Estimate;
import br.com.fiap.phase4.billing.domain.model.EstimateStatus;
import br.com.fiap.phase4.billing.domain.port.out.EstimateRepositoryPort;
import br.com.fiap.phase4.commons.domain.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("GetEstimateUseCase Unit Tests")
class GetEstimateUseCaseTest {

    @Mock
    private EstimateRepositoryPort repository;

    private GetEstimateUseCaseImpl useCase;

    @BeforeEach
    void setUp() {
        useCase = new GetEstimateUseCaseImpl(repository);
    }

    @Nested
    @DisplayName("Given an estimate lookup by primary ID")
    class GivenLookupById {

        @Nested
        @DisplayName("When the estimate exists in repository")
        class WhenEstimateExists {

            @Test
            @DisplayName("Should return the matching estimate domain entity")
            void shouldReturnMatchingEstimate() {
                UUID id = UUID.randomUUID();
                UUID workOrderId = UUID.randomUUID();
                Estimate expectedEstimate = Estimate.create(workOrderId, "52998224725", "customer@fiap.com.br", List.of());
                given(repository.findById(id)).willReturn(Optional.of(expectedEstimate));

                Estimate result = useCase.getById(id);

                assertThat(result).isNotNull();
                assertThat(result.getWorkOrderId()).isEqualTo(workOrderId);
                assertThat(result.getStatus()).isEqualTo(EstimateStatus.PENDING_APPROVAL);
            }
        }

        @Nested
        @DisplayName("When the estimate does not exist in repository")
        class WhenEstimateDoesNotExist {

            @Test
            @DisplayName("Should throw ResourceNotFoundException with estimate ID")
            void shouldThrowResourceNotFoundException() {
                UUID id = UUID.randomUUID();
                given(repository.findById(id)).willReturn(Optional.empty());

                assertThatThrownBy(() -> useCase.getById(id))
                        .isInstanceOf(ResourceNotFoundException.class)
                        .hasMessageContaining("Estimate")
                        .hasMessageContaining(id.toString());
            }
        }
    }

    @Nested
    @DisplayName("Given an estimate lookup by Work Order ID")
    class GivenLookupByWorkOrderId {

        @Nested
        @DisplayName("When an estimate exists for the specified work order")
        class WhenEstimateExistsForWorkOrder {

            @Test
            @DisplayName("Should return the corresponding estimate")
            void shouldReturnCorrespondingEstimate() {
                UUID workOrderId = UUID.randomUUID();
                Estimate expectedEstimate = Estimate.create(workOrderId, "52998224725", "customer@fiap.com.br", List.of());
                given(repository.findByWorkOrderId(workOrderId)).willReturn(Optional.of(expectedEstimate));

                Estimate result = useCase.getByWorkOrderId(workOrderId);

                assertThat(result).isNotNull();
                assertThat(result.getWorkOrderId()).isEqualTo(workOrderId);
            }
        }

        @Nested
        @DisplayName("When no estimate is associated with the work order")
        class WhenEstimateNotFoundForWorkOrder {

            @Test
            @DisplayName("Should throw ResourceNotFoundException with WorkOrderId")
            void shouldThrowResourceNotFoundException() {
                UUID workOrderId = UUID.randomUUID();
                given(repository.findByWorkOrderId(workOrderId)).willReturn(Optional.empty());

                assertThatThrownBy(() -> useCase.getByWorkOrderId(workOrderId))
                        .isInstanceOf(ResourceNotFoundException.class)
                        .hasMessageContaining("Estimate")
                        .hasMessageContaining(workOrderId.toString());
            }
        }
    }
}
