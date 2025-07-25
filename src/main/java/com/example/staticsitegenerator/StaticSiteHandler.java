package com.example.staticsitegenerator;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.util.List;
import java.util.Map;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;

import com.example.staticsitegenerator.CreateBucketRequest;
import com.example.staticsitegenerator.CreateAttachmentCustomDomainToBucketRequest;
import com.example.staticsitegenerator.StaticSiteResponseDTO;

// https://developers.cloudflare.com/api/resources/r2
// https://developers.cloudflare.com/r2/examples/aws/aws-sdk-java
// TODOs:
// - Split to cotrollers & services & repositories
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

    @Value("${cloudflare.r2.accessKey}")
    private String accessKey;

    @Value("${cloudflare.r2.secretKey}")
    private String secretKey;

    @Value("${cloudflare.r2.endpoint}")
    private String endpoint;

    public StaticSiteHandler() {
    }

    @PostMapping(
            value = "/generator",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<StaticSiteResponseDTO> createStaticSite(
        @RequestParam String name,
        @RequestParam("files") List<MultipartFile> files
    ) {
        try {
            generateBucket(name);
            attachCustomDomainToBucket(name);
            uploadFilesToBucket(name, files);
            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body(new StaticSiteResponseDTO("https://" + name + "." + domain + "/index.html"));
        } catch (Exception ex) {
            System.err.println("Error creating static site: " + ex.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
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

    private void uploadFilesToBucket(String bucketName, List<MultipartFile> files) throws Exception {
        AwsBasicCredentials credentials = AwsBasicCredentials.create(
            accessKey,
            secretKey
        );

        S3Configuration serviceConfiguration = S3Configuration.builder()
            .pathStyleAccessEnabled(true)
            .build();

        S3Client s3Client = S3Client.builder()
            .endpointOverride(URI.create(endpoint))
            .credentialsProvider(StaticCredentialsProvider.create(credentials))
            .region(Region.of("auto"))
            .serviceConfiguration(serviceConfiguration)
            .build();

        for (MultipartFile file : files) {
            Path tempFile = Files.createTempFile("upload", null);
            file.transferTo(tempFile);

            PutObjectRequest putRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(file.getOriginalFilename())
                .contentType(file.getContentType())
                .build();

            s3Client.putObject(putRequest, tempFile);
            Files.delete(tempFile);
        }
    }

    @DeleteMapping("/generator")
    public ResponseEntity<Void> deleteStaticSite(@RequestParam String name) {
        try {
            deleteBucket(name);     
            return ResponseEntity.noContent().build();
        } catch (Exception ex) {
            System.err.println("Error deleting bucket: " + ex.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    private void deleteBucket(String name) {
        // Empty the bucket first
        AwsBasicCredentials credentials = AwsBasicCredentials.create(
            accessKey,
            secretKey
        );

        S3Configuration serviceConfiguration = S3Configuration.builder()
            .pathStyleAccessEnabled(true)
            .build();

        S3Client s3Client = S3Client.builder()
            .endpointOverride(URI.create(endpoint))
            .credentialsProvider(StaticCredentialsProvider.create(credentials))
            .region(Region.of("auto"))
            .serviceConfiguration(serviceConfiguration)
            .build();

        ListObjectsV2Request listReq = ListObjectsV2Request.builder().bucket(name).build();
        ListObjectsV2Response listRes = s3Client.listObjectsV2(listReq);
        for (S3Object obj : listRes.contents()) {
            DeleteObjectRequest deleteReq = DeleteObjectRequest.builder()
                .bucket(name)
                .key(obj.key())
                .build();
            s3Client.deleteObject(deleteReq);
        }
        s3Client.close();

        // Delete empty bucket
        String url = "https://api.cloudflare.com/client/v4/accounts/" + accountId + "/r2/buckets/" + name;

        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        HttpEntity<Void> entity = new HttpEntity<>(headers);
        restTemplate.exchange(url, HttpMethod.DELETE, entity, Void.class);
    }
}
