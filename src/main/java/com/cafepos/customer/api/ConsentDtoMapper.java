package com.cafepos.customer.api;

import com.cafepos.customer.domain.ConsentRecord;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class ConsentDtoMapper {

    public ConsentRecordResponse toResponse(ConsentRecord record) {
        Objects.requireNonNull(record, "record must not be null");
        return new ConsentRecordResponse(
                record.getId(),
                record.getCafeId(),
                record.getCustomerId(),
                record.getConsentType(),
                record.isGranted(),
                record.getGrantedAt(),
                record.getRevokedAt(),
                record.getIpAddress(),
                record.getCreatedAt()
        );
    }
}
