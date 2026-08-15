package com.cafepos.customer.application;

import com.cafepos.customer.api.ConsentDtoMapper;
import com.cafepos.customer.api.ConsentRecordResponse;
import com.cafepos.customer.api.GrantConsentRequest;
import com.cafepos.customer.domain.ConsentRecord;
import com.cafepos.customer.domain.ConsentRecordRepository;
import com.cafepos.shared.error.BusinessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@Transactional
public class ConsentApplicationService {

    private final ConsentRecordRepository consentRepository;
    private final ConsentDtoMapper mapper;

    public ConsentApplicationService(ConsentRecordRepository consentRepository, ConsentDtoMapper mapper) {
        this.consentRepository = Objects.requireNonNull(consentRepository, "consentRepository must not be null");
        this.mapper = Objects.requireNonNull(mapper, "mapper must not be null");
    }

    public ConsentRecordResponse grantConsent(UUID cafeId, GrantConsentRequest request) {
        Objects.requireNonNull(cafeId, "cafeId must not be null");
        Objects.requireNonNull(request, "request must not be null");

        ConsentRecord record = new ConsentRecord(
                cafeId,
                request.customerId(),
                request.consentType(),
                true,
                request.ipAddress()
        );

        ConsentRecord saved = consentRepository.save(record);
        return mapper.toResponse(saved);
    }

    public ConsentRecordResponse revokeConsent(UUID cafeId, UUID consentId) {
        Objects.requireNonNull(cafeId, "cafeId must not be null");
        Objects.requireNonNull(consentId, "consentId must not be null");

        ConsentRecord record = consentRepository.findById(consentId)
                .filter(r -> r.getCafeId().equals(cafeId))
                .orElseThrow(() -> new BusinessException("CONSENT_NOT_FOUND", "Consent record not found with id " + consentId, 404));

        record.revoke();
        ConsentRecord saved = consentRepository.save(record);
        return mapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<ConsentRecordResponse> getCustomerConsents(UUID cafeId, UUID customerId) {
        return consentRepository.findByCafeIdAndCustomerId(cafeId, customerId).stream()
                .map(mapper::toResponse)
                .toList();
    }
}
