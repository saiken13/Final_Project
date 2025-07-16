package edu.hingu.project.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import edu.hingu.project.dtos.PricePredictionResponse;
import edu.hingu.project.dtos.PropertyInputDto;

@Service
public class PricePredictionService {
    private static final Logger logger = LoggerFactory.getLogger(PricePredictionService.class);
    private final RestTemplate restTemplate = new RestTemplate();

    public double predictPrice(PropertyInputDto input) {
        String url = "http://localhost:5001/predict"; // ML API URL
        logger.debug("Predicting price for input: {}", input);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<PropertyInputDto> request = new HttpEntity<>(input, headers);

        logger.debug("Sending request to {}", url);
        ResponseEntity<PricePredictionResponse> response = restTemplate.postForEntity(
            url,
            request,
            PricePredictionResponse.class
        );

        logger.debug("Received response: status={}, body={}", response.getStatusCode(), response.getBody());
        if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
            return response.getBody().getPredictedPrice();
        } else {
            logger.error("Prediction failed: status={}, body={}", response.getStatusCode(), response.getBody());
            throw new RuntimeException("Failed to get prediction from ML API: " + response.getStatusCode());
        }
    }
}