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
import com.example.staticsitegenerator.StaticSiteResponseDTO;

@RestController
public class StaticSiteHandler {
    @Value("${cloudflare.accountId}")
    private String accountId;

    @Value("${cloudflare.apiKey}")
    private String apiKey;

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
            // Create R2 bucket for each static site
            String url = "https://api.cloudflare.com/client/v4/accounts/" + accountId + "/r2/buckets";

            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            CreateBucketRequest createBucketRequest = new CreateBucketRequest(name, "eeur");
            HttpEntity<CreateBucketRequest> entity = new HttpEntity<>(createBucketRequest, headers);
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);
            if (response.getStatusCode() != HttpStatus.OK) {
                System.err.println("Failed to create bucket: " + response.getBody());
                return ResponseEntity
                        .status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(new StaticSiteResponseDTO("Failed to create bucket"));
            }

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
}
