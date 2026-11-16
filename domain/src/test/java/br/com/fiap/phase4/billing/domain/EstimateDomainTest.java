package br.com.fiap.phase4.billing.domain;

import br.com.fiap.phase4.billing.domain.model.Estimate;
import br.com.fiap.phase4.billing.domain.model.EstimateItem;
import br.com.fiap.phase4.billing.domain.model.EstimateItemType;
import br.com.fiap.phase4.billing.domain.model.EstimateStatus;
import br.com.fiap.phase4.billing.domain.port.out.EstimateEventPublisherPort;
import br.com.fiap.phase4.billing.domain.port.out.EstimateNotificationPort;
import br.com.fiap.phase4.billing.domain.port.out.EstimateRepositoryPort;
import br.com.fiap.phase4.billing.domain.usecase.ApproveEstimateUseCaseImpl;
import br.com.fiap.phase4.billing.domain.usecase.CreateEstimateUseCaseImpl;
import br.com.fiap.phase4.billing.domain.usecase.GetEstimateUseCaseImpl;
import br.com.fiap.phase4.commons.domain.exception.DomainException;
import br.com.fiap.phase4.commons.domain.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Estimate Domain Unit Tests")
class EstimateDomainTest {

    @Mock
    private EstimateRepositoryPort repository;

    @Mock
    private EstimateEventPublisherPort eventPublisher;

    @Mock
    private EstimateNotificationPort notificationPort;

    private CreateEstimateUseCaseImpl createUseCase;
    private ApproveEstimateUseCaseImpl approveUseCase;
    private GetEstimateUseCaseImpl getUseCase;

    @BeforeEach
    void setUp() {
        createUseCase = new CreateEstimateUseCaseImpl(repository, eventPublisher, notificationPort);
        approveUseCase = new ApproveEstimateUseCaseImpl(repository, eventPublisher);
        getUseCase = new GetEstimateUseCaseImpl(repository);
    }

    @Nested
    @DisplayName("Estimate Creation Tests")
    class CreationTests {

        @Test
        @DisplayName("Should create estimate and send proposal notification")
        void shouldCreateEstimateSuccessfully() {
            UUID workOrderId = UUID.randomUUID();
            String doc = "52998224725";
            String email = "customer@fiap.com.br";

            EstimateItem service = EstimateItem.of(null, EstimateItemType.SERVICE, "SRV-01", "Brake Pad Change", new BigDecimal("150.00"), 1);
            EstimateItem material = EstimateItem.of(null, EstimateItemType.MATERIAL, "MAT-01", "Brake Pad Pair", new BigDecimal("250.00"), 1);

            when(repository.findByWorkOrderId(workOrderId)).thenReturn(Optional.empty());
            when(repository.save(any(Estimate.class))).thenAnswer(inv -> inv.getArgument(0));

            Estimate created = createUseCase.createEstimate(workOrderId, doc, email, List.of(service, material));

            assertThat(created).isNotNull();
            assertThat(created.getWorkOrderId()).isEqualTo(workOrderId);
            assertThat(created.getStatus()).isEqualTo(EstimateStatus.PENDING_APPROVAL);
            assertThat(created.getTotalAmount()).isEqualByComparingTo(new BigDecimal("400.00"));
            assertThat(created.getItems()).hasSize(2);

            verify(notificationPort, times(1)).sendEstimateProposalEmail(any(Estimate.class));
            verify(eventPublisher, times(1)).publishEstimateCreated(any(Estimate.class), isNull());
        }

        @Test
        @DisplayName("Should return existing estimate when already created (Idempotency)")
        void shouldReturnExistingEstimateIdempotently() {
            UUID workOrderId = UUID.randomUUID();
            Estimate existing = Estimate.create(workOrderId, "52998224725", "customer@fiap.com.br", List.of());

            when(repository.findByWorkOrderId(workOrderId)).thenReturn(Optional.of(existing));

            Estimate result = createUseCase.createEstimate(workOrderId, "52998224725", "customer@fiap.com.br", List.of());

            assertThat(result).isSameAs(existing);
            verify(repository, never()).save(any());
            verify(notificationPort, never()).sendEstimateProposalEmail(any());
        }
    }

    @Nested
    @DisplayName("Estimate Sovereign Decision Tests")
    class DecisionTests {

        @Test
        @DisplayName("Should approve estimate and publish EstimateApprovedEvent")
        void shouldApproveEstimate() {
            UUID workOrderId = UUID.randomUUID();
            Estimate estimate = Estimate.create(workOrderId, "52998224725", "customer@fiap.com.br", List.of());

            when(repository.findByWorkOrderId(workOrderId)).thenReturn(Optional.of(estimate));
            when(repository.save(any(Estimate.class))).thenAnswer(inv -> inv.getArgument(0));

            Estimate approved = approveUseCase.approveByWorkOrderId(workOrderId, "trace-123");

            assertThat(approved.getStatus()).isEqualTo(EstimateStatus.APPROVED);
            verify(eventPublisher, times(1)).publishEstimateApproved(approved, "trace-123");
        }

        @Test
        @DisplayName("Should reject estimate and publish EstimateRejectedEvent")
        void shouldRejectEstimate() {
            UUID workOrderId = UUID.randomUUID();
            Estimate estimate = Estimate.create(workOrderId, "52998224725", "customer@fiap.com.br", List.of());

            when(repository.findByWorkOrderId(workOrderId)).thenReturn(Optional.of(estimate));
            when(repository.save(any(Estimate.class))).thenAnswer(inv -> inv.getArgument(0));

            Estimate rejected = approveUseCase.rejectByWorkOrderId(workOrderId, "Price is too high", "trace-456");

            assertThat(rejected.getStatus()).isEqualTo(EstimateStatus.REJECTED);
            assertThat(rejected.getRejectionReason()).isEqualTo("Price is too high");
            verify(eventPublisher, times(1)).publishEstimateRejected(rejected, "trace-456");
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when approving non-existent estimate")
        void shouldThrowWhenEstimateNotFound() {
            UUID workOrderId = UUID.randomUUID();
            when(repository.findByWorkOrderId(workOrderId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> approveUseCase.approveByWorkOrderId(workOrderId, "trace-123"))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("Should throw DomainException when approving already rejected estimate")
        void shouldThrowWhenApprovingRejectedEstimate() {
            UUID workOrderId = UUID.randomUUID();
            Estimate estimate = Estimate.create(workOrderId, "52998224725", "customer@fiap.com.br", List.of());
            estimate.reject("Declined");

            when(repository.findByWorkOrderId(workOrderId)).thenReturn(Optional.of(estimate));

            assertThatThrownBy(() -> approveUseCase.approveByWorkOrderId(workOrderId, "trace-123"))
                    .isInstanceOf(DomainException.class)
                    .hasMessageContaining("Cannot approve an already rejected estimate");
        }
    }

    @Nested
    @DisplayName("Estimate Query Tests")
    class QueryTests {

        @Test
        @DisplayName("Should retrieve estimate by workOrderId")
        void shouldGetByWorkOrderId() {
            UUID workOrderId = UUID.randomUUID();
            Estimate estimate = Estimate.create(workOrderId, "52998224725", "customer@fiap.com.br", List.of());
            when(repository.findByWorkOrderId(workOrderId)).thenReturn(Optional.of(estimate));

            Estimate found = getUseCase.getByWorkOrderId(workOrderId);

            assertThat(found).isNotNull();
            assertThat(found.getWorkOrderId()).isEqualTo(workOrderId);
        }
    }
}
