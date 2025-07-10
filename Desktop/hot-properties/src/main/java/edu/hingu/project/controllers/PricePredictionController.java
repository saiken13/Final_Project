package edu.hingu.project.controllers;


import java.security.Principal;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
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

    @Autowired
    private PricePredictionService predictionService;

    @PostMapping
    public ResponseEntity<PricePredictionResponse> predictPrice(@RequestBody PropertyInputDto input) {
        double predicted = predictionService.predictPrice(input);

        PricePredictionResponse response = new PricePredictionResponse();
        response.setPredictedPrice(predicted);

        return ResponseEntity.ok(response);
    }

    @Autowired
    private PredictionHistoryService predictionHistoryService;

    @PostMapping("/save")
    public ResponseEntity<Void> savePrediction(@RequestBody PropertyInputDto input, Principal principal) {
        predictionHistoryService.savePrediction(input, principal.getName());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/history")
    public ResponseEntity<List<PredictionHistory>> getHistory(Principal principal) {
        List<PredictionHistory> history = predictionHistoryService.getUserHistory(principal.getName());
        return ResponseEntity.ok(history);
    }

    @GetMapping("/history/view")
    public String showPredictionHistory(Model model, Principal principal) {
        String email = principal.getName(); // 👈 current logged-in user's email
        List<PredictionHistory> historyList = predictionHistoryService.getUserHistory(email);

        model.addAttribute("predictionHistoryList", historyList); // ✅ this is critical
        return "prediction-history"; // corresponds to prediction-history.html
    }



}

