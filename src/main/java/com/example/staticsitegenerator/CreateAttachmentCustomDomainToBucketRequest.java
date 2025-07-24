package com.example.staticsitegenerator;

public class CreateAttachmentCustomDomainToBucketRequest {
    private String domain;
    private boolean enabled;
    private String zoneId;

    public CreateAttachmentCustomDomainToBucketRequest() {
    }

    public CreateAttachmentCustomDomainToBucketRequest(String domain, boolean enabled, String zoneId) {
        this.domain = domain;
        this.enabled = enabled;
        this.zoneId = zoneId;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getZoneId() {
        return zoneId;
    }

    public void setZoneId(String zoneId) {
        this.zoneId = zoneId;
    }
}
