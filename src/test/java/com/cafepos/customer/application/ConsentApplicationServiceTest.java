package com.cafepos.customer.application;

import com.cafepos.customer.api.ConsentDtoMapper;
import com.cafepos.customer.api.ConsentRecordResponse;
import com.cafepos.customer.api.GrantConsentRequest;
import com.cafepos.customer.domain.ConsentRecord;
import com.cafepos.customer.domain.ConsentRecordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConsentApplicationServiceTest {

    @Mock
    private ConsentRecordRepository consentRepository;

    private final ConsentDtoMapper mapper = new ConsentDtoMapper();
    private ConsentApplicationService consentApplicationService;

    private final UUID cafeId = UUID.randomUUID();
    private final UUID customerId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        consentApplicationService = new ConsentApplicationService(consentRepository, mapper);
    }

    @Test
    @DisplayName("Should grant consent successfully and save entity")
    void grantConsentSuccess() {
        GrantConsentRequest request = new GrantConsentRequest(customerId, "MARKETING_EMAIL", "127.0.0.1");

        when(consentRepository.save(any(ConsentRecord.class))).thenAnswer(inv -> inv.getArgument(0));

        ConsentRecordResponse response = consentApplicationService.grantConsent(cafeId, request);

        assertThat(response).isNotNull();
        assertThat(response.cafeId()).isEqualTo(cafeId);
        assertThat(response.customerId()).isEqualTo(customerId);
        assertThat(response.consentType()).isEqualTo("MARKETING_EMAIL");
        assertThat(response.granted()).isTrue();
        verify(consentRepository).save(any(ConsentRecord.class));
    }

    @Test
    @DisplayName("Should revoke consent successfully")
    void revokeConsentSuccess() {
        UUID consentId = UUID.randomUUID();
        ConsentRecord record = new ConsentRecord(consentId, cafeId, customerId, "MARKETING_EMAIL", true, "127.0.0.1");

        when(consentRepository.findById(consentId)).thenReturn(Optional.of(record));
        when(consentRepository.save(any(ConsentRecord.class))).thenAnswer(inv -> inv.getArgument(0));

        ConsentRecordResponse response = consentApplicationService.revokeConsent(cafeId, consentId);

        assertThat(response).isNotNull();
        assertThat(response.granted()).isFalse();
        assertThat(response.revokedAt()).isNotNull();
        verify(consentRepository).save(record);
    }
}
