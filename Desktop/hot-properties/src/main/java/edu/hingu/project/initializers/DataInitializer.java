package edu.hingu.project.initializers;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import edu.hingu.project.entities.Property;
import edu.hingu.project.entities.User;
import edu.hingu.project.repositories.PropertyRepository;
import edu.hingu.project.repositories.UserRepository;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired private PropertyRepository propertyRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) {
        System.out.println("Cleaning DB for dev...");

        jdbcTemplate.execute("DELETE FROM favorite");
        jdbcTemplate.execute("DELETE FROM property_image");
        jdbcTemplate.execute("DELETE FROM properties");
        jdbcTemplate.execute("ALTER TABLE properties AUTO_INCREMENT = 1");

        List<User> agents = userRepository.findAll().stream()
            .filter(u -> u.getRoles().stream().anyMatch(r -> r.getName().equalsIgnoreCase("AGENT")))
            .collect(Collectors.toList());

        if (agents.isEmpty()) {
            System.err.println("No AGENT users found.");
            return;
        }

        System.out.println("Loading homedata.csv and assigning to agents...");

        try (
            CSVParser parser = CSVParser.parse(
                new ClassPathResource("homedata.csv").getInputStream(),
                StandardCharsets.UTF_8,
                CSVFormat.DEFAULT.withFirstRecordAsHeader()
            )
        ) {
            int agentIndex = 0;
            int count = 0;
            Set<String> insertedTitles = new HashSet<>();

            for (CSVRecord record : parser) {
                try {
                    String title = record.get("title").trim();
                    String folderName = title.replaceAll(" ", "_");

                    if (insertedTitles.contains(title)) continue;

                    File folder = new File("src/main/resources/static/images/property-images/" + folderName);
                    if (!folder.exists()) {
                        System.err.println(" Skipping: Missing image folder -> " + folderName);
                        continue;
                    }

                    Property property = new Property();
                    property.setTitle(title);
                    property.setPrice(Double.parseDouble(record.get("price").trim()));
                    property.setLocation(record.get("location").trim());
                    property.setSize(Integer.parseInt(record.get("size").trim()));
                    property.setDescription(record.get("description").trim());
                    property.setImageFolder(folderName);

                    User agent = agents.get(agentIndex % agents.size());
                    property.setOwner(agent);
                    agentIndex++;

                    propertyRepository.save(property);
                    insertedTitles.add(title);
                    count++;
                } catch (Exception e) {
                    System.err.println(" Skipped a row: " + e.getMessage());
                }
            }

            System.out.println(" Loaded " + count + " properties.");
        } catch (Exception e) {
            System.err.println(" CSV read error: " + e.getMessage());
        }
    }
}
