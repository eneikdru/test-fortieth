package com.eneik.generated.integration;

public class LmsDocumentMetadataDto {
    private String externalId;
    private String metadataKey;
    private String metadataValue;

    public LmsDocumentMetadataDto() {}

    public LmsDocumentMetadataDto(String externalId, String metadataKey, String metadataValue) {
        this.externalId = externalId;
        this.metadataKey = metadataKey;
        this.metadataValue = metadataValue;
    }

    public String getExternalId() {
        return externalId;
    }

    public void setExternalId(String externalId) {
        this.externalId = externalId;
    }

    public String getMetadataKey() {
        return metadataKey;
    }

    public void setMetadataKey(String metadataKey) {
        this.metadataKey = metadataKey;
    }

    public String getMetadataValue() {
        return metadataValue;
    }

    public void setMetadataValue(String metadataValue) {
        this.metadataValue = metadataValue;
    }
}
