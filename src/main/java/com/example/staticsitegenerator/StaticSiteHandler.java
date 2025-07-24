package com.example.staticsitegenerator;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

import com.example.staticsitegenerator.CreateBucketRequest;
import com.example.staticsitegenerator.CreateAttachmentCustomDomainToBucketRequest;
import com.example.staticsitegenerator.StaticSiteResponseDTO;

// TODOs:
// - Add error handling for file uploads
// - Use https://resilience4j.readme.io for retry logic
@RestController
public class StaticSiteHandler {
    @Value("${cloudflare.zoneId}")
    private String zoneId;

    @Value("${cloudflare.accountId}")
    private String accountId;

    @Value("${cloudflare.apiKey}")
    private String apiKey;

    @Value("${cloudflare.domain}")
    private String domain;

    public StaticSiteHandler() {
    }

    @PostMapping(
            value = "/generator",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<StaticSiteResponseDTO> createStaticSite(
        @RequestParam String name,
        @RequestParam String owner,
        @RequestParam("files") List<MultipartFile> files
    ) {
        try {
            generateBucket(name);
            attachCustomDomainToBucket(name);

            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body(new StaticSiteResponseDTO("valid url"));
        } catch (Exception ex) {
            System.err.println("Error creating static site: " + ex.getMessage());
            return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new StaticSiteResponseDTO("invalid url"));
        }
    }

    private void generateBucket(String name) {
        String url = "https://api.cloudflare.com/client/v4/accounts/" + accountId + "/r2/buckets";

        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        CreateBucketRequest requestBody = new CreateBucketRequest(name, "eeur");
        HttpEntity<CreateBucketRequest> entity = new HttpEntity<>(requestBody, headers);
        ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);
    }

    private void attachCustomDomainToBucket(String bucketName) {
        String url = "https://api.cloudflare.com/client/v4/accounts/" + accountId + "/r2/buckets/" + bucketName + "/domains/custom";

        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        CreateAttachmentCustomDomainToBucketRequest requestBody = new CreateAttachmentCustomDomainToBucketRequest(
                bucketName + "." + domain, 
                true, 
                zoneId
        );
        HttpEntity<CreateAttachmentCustomDomainToBucketRequest> entity = new HttpEntity<>(requestBody, headers);
        ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);
    }
}
