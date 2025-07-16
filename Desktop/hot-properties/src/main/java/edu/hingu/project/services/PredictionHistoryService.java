package edu.hingu.project.services;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import edu.hingu.project.dtos.PropertyInputDto;
import edu.hingu.project.entities.PredictionHistory;
import edu.hingu.project.entities.User;
import edu.hingu.project.repositories.PredictionHistoryRepository;
import edu.hingu.project.repositories.UserRepository;

@Service
public class PredictionHistoryService {

    @Autowired
    private PredictionHistoryRepository historyRepo;

    @Autowired
    private UserRepository userRepo;

    @Autowired // ✅ ADD THIS
    private PricePredictionService predictionService; // ✅ ADD THIS FIELD

    public void savePrediction(PropertyInputDto input, String email) {
        User user = userRepo.findByEmail(email)
            .orElseThrow(() -> new RuntimeException("User not found with email: " + email));

        double predictedPrice = predictionService.predictPrice(input); // ✅ This now works

        PredictionHistory history = new PredictionHistory();
        history.setBedrooms(input.getBedrooms());
        history.setBathrooms(input.getBathrooms());
        history.setArea(input.getArea());
        history.setPredictedPrice(predictedPrice);
        history.setTimestamp(LocalDateTime.now());
        history.setUser(user);

        historyRepo.save(history);
    }

    public List<PredictionHistory> getUserHistory(String email) {
        User user = userRepo.findByEmail(email).orElseThrow(() ->
            new RuntimeException("User not found with email: " + email)
        );
        return historyRepo.findByUser(user);
    }
}