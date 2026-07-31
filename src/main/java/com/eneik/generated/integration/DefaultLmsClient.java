package com.eneik.generated.integration;

import org.springframework.stereotype.Component;
import java.util.Collections;
import java.util.List;

@Component
public class DefaultLmsClient implements LmsClient {
    @Override
    public List<LmsDocumentMetadataDto> fetchUpdatedDocumentMetadata() {
        return Collections.emptyList();
    }
}
