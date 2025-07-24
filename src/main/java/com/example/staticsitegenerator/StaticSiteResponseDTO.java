package com.example.staticsitegenerator;

public class StaticSiteResponseDTO {
    private String url;

    public StaticSiteResponseDTO(String url) {
        this.url = url;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}
