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
            assertThat(created.getCustomerDocument()).isEqualTo(doc);
            assertThat(created.getCustomerEmail()).isEqualTo(email);
            assertThat(created.getTotalAmount()).isEqualByComparingTo("400.00");
            assertThat(created.getStatus()).isEqualTo(EstimateStatus.PENDING_APPROVAL);
            assertThat(created.getItems()).hasSize(2);

            verify(repository, times(1)).save(any(Estimate.class));
            verify(notificationPort, times(1)).sendEstimateProposalEmail(created);
        }

        @Test
        @DisplayName("Should return existing estimate idempotently if already present")
        void shouldReturnExistingEstimateIdempotently() {
            UUID workOrderId = UUID.randomUUID();
            Estimate existing = Estimate.create(workOrderId, "52998224725", "customer@fiap.com.br", List.of());
            when(repository.findByWorkOrderId(workOrderId)).thenReturn(Optional.of(existing));

            Estimate result = createUseCase.createEstimate(workOrderId, "52998224725", "customer@fiap.com.br", List.of());

            assertThat(result).isSameAs(existing);
            verify(repository, never()).save(any(Estimate.class));
            verifyNoInteractions(notificationPort);
        }

        @Test
        @DisplayName("Should throw NullPointerException when workOrderId or customerDocument is null")
        void shouldThrowWhenRequiredFieldMissing() {
            assertThatThrownBy(() -> Estimate.create(null, "52998224725", "email@fiap.com", List.of()))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("Work Order ID is required");

            UUID workOrderId = UUID.randomUUID();
            assertThatThrownBy(() -> Estimate.create(workOrderId, null, "email@fiap.com", List.of()))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("Customer document is required");
        }

        @Test
        @DisplayName("Should create estimate with null items list defaulting total to zero")
        void shouldHandleNullItemsList() {
            UUID workOrderId = UUID.randomUUID();
            Estimate estimate = Estimate.create(workOrderId, "52998224725", "email@fiap.com", null);

            assertThat(estimate.getTotalAmount()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(estimate.getItems()).isEmpty();
        }
    }

    @Nested
    @DisplayName("Estimate Sovereign Decision Tests")
    class SovereignDecisionTests {

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
        @DisplayName("Should be idempotent when approving already approved estimate")
        void shouldBeIdempotentWhenApprovingAlreadyApproved() {
            UUID workOrderId = UUID.randomUUID();
            Estimate estimate = Estimate.create(workOrderId, "52998224725", "customer@fiap.com.br", List.of());
            estimate.approve();

            when(repository.findByWorkOrderId(workOrderId)).thenReturn(Optional.of(estimate));
            when(repository.save(any(Estimate.class))).thenAnswer(inv -> inv.getArgument(0));

            Estimate approvedAgain = approveUseCase.approveByWorkOrderId(workOrderId, "trace-123");

            assertThat(approvedAgain.getStatus()).isEqualTo(EstimateStatus.APPROVED);
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
        @DisplayName("Should be idempotent when rejecting already rejected estimate")
        void shouldBeIdempotentWhenRejectingAlreadyRejected() {
            UUID workOrderId = UUID.randomUUID();
            Estimate estimate = Estimate.create(workOrderId, "52998224725", "customer@fiap.com.br", List.of());
            estimate.reject("Initial reason");

            when(repository.findByWorkOrderId(workOrderId)).thenReturn(Optional.of(estimate));
            when(repository.save(any(Estimate.class))).thenAnswer(inv -> inv.getArgument(0));

            Estimate rejectedAgain = approveUseCase.rejectByWorkOrderId(workOrderId, "Second reason", "trace-456");

            assertThat(rejectedAgain.getStatus()).isEqualTo(EstimateStatus.REJECTED);
            assertThat(rejectedAgain.getRejectionReason()).isEqualTo("Initial reason");
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
        @DisplayName("Should throw ResourceNotFoundException when rejecting non-existent estimate")
        void shouldThrowWhenRejectingNotFoundEstimate() {
            UUID workOrderId = UUID.randomUUID();
            when(repository.findByWorkOrderId(workOrderId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> approveUseCase.rejectByWorkOrderId(workOrderId, "Reason", "trace-123"))
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

        @Test
        @DisplayName("Should throw DomainException when rejecting already approved estimate")
        void shouldThrowWhenRejectingApprovedEstimate() {
            UUID workOrderId = UUID.randomUUID();
            Estimate estimate = Estimate.create(workOrderId, "52998224725", "customer@fiap.com.br", List.of());
            estimate.approve();

            when(repository.findByWorkOrderId(workOrderId)).thenReturn(Optional.of(estimate));

            assertThatThrownBy(() -> approveUseCase.rejectByWorkOrderId(workOrderId, "Decline after approve", "trace-123"))
                    .isInstanceOf(DomainException.class)
                    .hasMessageContaining("Cannot reject an already approved estimate");
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

        @Test
        @DisplayName("Should throw ResourceNotFoundException when getByWorkOrderId not found")
        void shouldThrowWhenGetByWorkOrderIdNotFound() {
            UUID workOrderId = UUID.randomUUID();
            when(repository.findByWorkOrderId(workOrderId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> getUseCase.getByWorkOrderId(workOrderId))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Estimate")
                    .hasMessageContaining(workOrderId.toString());
        }

        @Test
        @DisplayName("Should retrieve estimate by primary ID")
        void shouldGetById() {
            UUID id = UUID.randomUUID();
            Estimate estimate = Estimate.create(UUID.randomUUID(), "52998224725", "customer@fiap.com.br", List.of());
            when(repository.findById(id)).thenReturn(Optional.of(estimate));

            Estimate found = getUseCase.getById(id);

            assertThat(found).isNotNull();
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when getById not found")
        void shouldThrowWhenGetByIdNotFound() {
            UUID id = UUID.randomUUID();
            when(repository.findById(id)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> getUseCase.getById(id))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Estimate")
                    .hasMessageContaining(id.toString());
        }
    }
}
