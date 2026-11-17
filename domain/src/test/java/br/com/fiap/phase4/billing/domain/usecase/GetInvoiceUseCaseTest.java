package br.com.fiap.phase4.billing.domain.usecase;

import br.com.fiap.phase4.billing.domain.model.Invoice;
import br.com.fiap.phase4.billing.domain.model.InvoiceStatus;
import br.com.fiap.phase4.billing.domain.port.out.InvoiceRepositoryPort;
import br.com.fiap.phase4.commons.domain.exception.ResourceNotFoundException;
import br.com.fiap.phase4.commons.domain.vo.CpfOrCnpj;
import br.com.fiap.phase4.commons.domain.vo.Money;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("GetInvoiceUseCase Unit Tests")
class GetInvoiceUseCaseTest {

    @Mock
    private InvoiceRepositoryPort repository;

    private GetInvoiceUseCaseImpl useCase;

    @BeforeEach
    void setUp() {
        useCase = new GetInvoiceUseCaseImpl(repository);
    }

    @Nested
    @DisplayName("Given an invoice lookup by primary ID")
    class GivenLookupById {

        @Nested
        @DisplayName("When the invoice exists in repository")
        class WhenInvoiceExists {

            @Test
            @DisplayName("Should return the matching invoice domain entity")
            void shouldReturnMatchingInvoice() {
                UUID id = UUID.randomUUID();
                UUID workOrderId = UUID.randomUUID();
                Invoice expectedInvoice = Invoice.create(workOrderId, new CpfOrCnpj("52998224725"), Money.of(new BigDecimal("350.00")));
                given(repository.findById(id)).willReturn(Optional.of(expectedInvoice));

                Invoice result = useCase.getById(id);

                assertThat(result).isNotNull();
                assertThat(result.getWorkOrderId()).isEqualTo(workOrderId);
                assertThat(result.getStatus()).isEqualTo(InvoiceStatus.PENDING);
            }
        }

        @Nested
        @DisplayName("When the invoice does not exist in repository")
        class WhenInvoiceDoesNotExist {

            @Test
            @DisplayName("Should throw ResourceNotFoundException with invoice ID")
            void shouldThrowResourceNotFoundException() {
                UUID id = UUID.randomUUID();
                given(repository.findById(id)).willReturn(Optional.empty());

                assertThatThrownBy(() -> useCase.getById(id))
                        .isInstanceOf(ResourceNotFoundException.class)
                        .hasMessageContaining("Invoice")
                        .hasMessageContaining(id.toString());
            }
        }
    }

    @Nested
    @DisplayName("Given an invoice lookup by Work Order ID")
    class GivenLookupByWorkOrderId {

        @Nested
        @DisplayName("When an invoice exists for the specified work order")
        class WhenInvoiceExistsForWorkOrder {

            @Test
            @DisplayName("Should return the corresponding invoice")
            void shouldReturnCorrespondingInvoice() {
                UUID workOrderId = UUID.randomUUID();
                Invoice expectedInvoice = Invoice.create(workOrderId, new CpfOrCnpj("52998224725"), Money.of(new BigDecimal("499.90")));
                given(repository.findByWorkOrderId(workOrderId)).willReturn(Optional.of(expectedInvoice));

                Invoice result = useCase.getByWorkOrderId(workOrderId);

                assertThat(result).isNotNull();
                assertThat(result.getWorkOrderId()).isEqualTo(workOrderId);
                assertThat(result.getAmount().amount()).isEqualByComparingTo("499.90");
            }
        }

        @Nested
        @DisplayName("When no invoice is associated with the work order")
        class WhenInvoiceNotFoundForWorkOrder {

            @Test
            @DisplayName("Should throw ResourceNotFoundException with WorkOrder description")
            void shouldThrowResourceNotFoundException() {
                UUID workOrderId = UUID.randomUUID();
                given(repository.findByWorkOrderId(workOrderId)).willReturn(Optional.empty());

                assertThatThrownBy(() -> useCase.getByWorkOrderId(workOrderId))
                        .isInstanceOf(ResourceNotFoundException.class)
                        .hasMessageContaining("Invoice for WorkOrder")
                        .hasMessageContaining(workOrderId.toString());
            }
        }
    }
}
