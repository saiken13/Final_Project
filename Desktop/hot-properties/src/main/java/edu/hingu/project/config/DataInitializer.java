package edu.hingu.project.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import edu.hingu.project.entities.Property;
import edu.hingu.project.repositories.PropertyRepository;
import jakarta.annotation.PostConstruct;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private PropertyRepository propertyRepository;

    @PostConstruct
    public void updateAllImageFolders() {
        List<Property> properties = propertyRepository.findAll();

        for (Property p : properties) {
            String cleanFolder = p.getTitle().replaceAll(" ", "_");
            p.setImageFolder(cleanFolder);
        }

        propertyRepository.saveAll(properties);
        System.out.println("✅ Updated all image folder names based on property title.");
    }

    @Override
    public void run(String... args) {
        updateAllImageFolders(); // Force update for existing properties
        System.out.println("✅ Synced image folder names");
        
        // Uncomment this block below if you ever need to re-seed from CSV
        /*
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
                    try {
                        Property property = new Property();
                        property.setTitle(record.get("title").trim());
                        property.setPrice(Double.parseDouble(record.get("price").trim()));
                        property.setLocation(record.get("location").trim());
                        property.setSize(Integer.parseInt(record.get("size").trim()));
                        property.setDescription(record.get("description").trim());

                        // Raw folder for now — updated later by @PostConstruct
                        property.setImageFolder(property.getTitle());

                        propertyRepository.save(property);
                    } catch (Exception e) {
                        System.err.println("Skipping row due to error: " + e.getMessage());
                    }
                }
                System.out.println("✅ Property data loaded successfully.");
            } catch (Exception e) {
                System.err.println("❌ Error loading data: " + e.getMessage());
            }
        } else {
            System.out.println("🟡 Properties already exist, skipping initialization.");
        }
        */
    }
}
