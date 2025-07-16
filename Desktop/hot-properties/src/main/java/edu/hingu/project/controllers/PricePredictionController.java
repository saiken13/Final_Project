package edu.hingu.project.controllers;

import java.security.Principal;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import edu.hingu.project.dtos.PricePredictionResponse;
import edu.hingu.project.dtos.PropertyInputDto;
import edu.hingu.project.entities.PredictionHistory;
import edu.hingu.project.services.PredictionHistoryService;
import edu.hingu.project.services.PricePredictionService;

@RestController
@RequestMapping("/api/predict")
public class PricePredictionController {
    private static final Logger logger = LoggerFactory.getLogger(PricePredictionController.class);

    @Autowired
    private PricePredictionService predictionService;

    @PostMapping
    public ResponseEntity<PricePredictionResponse> predictPrice(@RequestBody PropertyInputDto input) {
        logger.debug("Received POST request to /api/predict with method: {}", "POST");
        double predicted = predictionService.predictPrice(input);
        PricePredictionResponse response = new PricePredictionResponse();
        response.setPredictedPrice(predicted);
        return ResponseEntity.ok(response);
    }

    @Autowired
    private PredictionHistoryService predictionHistoryService;

    @PostMapping("/save")
    public ResponseEntity<Void> savePrediction(@RequestBody PropertyInputDto input, Principal principal) {
        logger.debug("Received POST request to /api/predict/save");
        predictionHistoryService.savePrediction(input, principal.getName());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/history")
    public ResponseEntity<List<PredictionHistory>> getHistory(Principal principal) {
        logger.debug("Received GET request to /api/predict/history");
        List<PredictionHistory> history = predictionHistoryService.getUserHistory(principal.getName());
        return ResponseEntity.ok(history);
    }
}