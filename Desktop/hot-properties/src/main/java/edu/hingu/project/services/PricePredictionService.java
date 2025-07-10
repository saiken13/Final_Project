package edu.hingu.project.services;

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
    

    private final RestTemplate restTemplate = new RestTemplate();

    public double predictPrice(PropertyInputDto input) {
        String url = "http://localhost:5000/predict"; // ML API URL

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<PropertyInputDto> request = new HttpEntity<>(input, headers);

        ResponseEntity<PricePredictionResponse> response = restTemplate.postForEntity(
            url,
            request,
            PricePredictionResponse.class
        );

        if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
            return response.getBody().getPredictedPrice();
        } else {
            throw new RuntimeException("Failed to get prediction from ML API");
        }
    }
}
