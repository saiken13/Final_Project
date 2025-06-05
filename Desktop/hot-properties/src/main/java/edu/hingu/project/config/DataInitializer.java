package edu.hingu.project.config;

import java.nio.charset.StandardCharsets;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import edu.hingu.project.entities.Property;
import edu.hingu.project.repositories.PropertyRepository;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private PropertyRepository propertyRepository;



    @Override
    public void run(String... args) throws Exception {
        if (propertyRepository.count() == 0) {
            System.out.println("Loading property data from CSV...");

            try (
                CSVParser parser = CSVParser.parse(
                    new ClassPathResource("homedata.csv").getInputStream(),
                    StandardCharsets.UTF_8,
                    CSVFormat.DEFAULT.withFirstRecordAsHeader()
                )
            ) {
                for (CSVRecord record : parser) {
                    Property property = new Property();
                    property.setTitle(record.get("title").trim());
                    property.setPrice(Double.parseDouble(record.get("price").trim()));
                    property.setLocation(record.get("location").trim());
                    property.setSize(Integer.parseInt(record.get("size").trim()));
                    property.setDescription(record.get("description").trim());

                    propertyRepository.save(property);
                }
                System.out.println("Property data loaded successfully.");
            } catch (Exception e) {
                System.err.println("Error loading data: " + e.getMessage());
            }
        }
    }
}
