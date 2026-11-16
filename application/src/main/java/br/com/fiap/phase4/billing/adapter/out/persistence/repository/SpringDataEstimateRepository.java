package br.com.fiap.phase4.billing.adapter.out.persistence.repository;

import br.com.fiap.phase4.billing.domain.model.Estimate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SpringDataEstimateRepository extends JpaRepository<Estimate, UUID> {

    Optional<Estimate> findByWorkOrderId(UUID workOrderId);

    boolean existsByWorkOrderId(UUID workOrderId);
}
