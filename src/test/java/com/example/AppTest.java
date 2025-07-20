
package com.example;

import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class AppTest {
    private static final ObjectMapper mapper = new ObjectMapper();
    private static final JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7);

    private static final String SCHEMAS_DIR = "src/main/resources/schemas";
    private static final String INSTANCES_DIR = "src/main/resources/instances";

    @BeforeClass
    public static void setup() {
        // Any setup if needed, e.g., generate relationships.json if required for tests
    }

    @Test
    public void testSchemaValidationForAllInstances() throws IOException {
        try (Stream<Path> paths = Files.walk(Paths.get(INSTANCES_DIR))) {
            paths.filter(Files::isRegularFile)
                    .forEach(path -> {
                        try {
                            String subDir = path.getParent().getFileName().toString();
                            String fileName = path.getFileName().toString();
                            String schemaType = fileName.substring(0, fileName.lastIndexOf('-'));
                            Path schemaPath = Paths.get(SCHEMAS_DIR, subDir, schemaType + ".json");

                            JsonNode instance = mapper.readTree(path.toFile());
                            JsonSchema schema = factory.getSchema(Files.newInputStream(schemaPath));
                            Set<ValidationMessage> errors = schema.validate(instance);

                            assertTrue("Validation errors for " + fileName + ": " + errors, errors.isEmpty());
                        } catch (IOException e) {
                            fail("Error processing " + path + ": " + e.getMessage());
                        }
                    });
        }
    }

    @Test
    public void testContentValidationForEngines() throws IOException {
        try (Stream<Path> paths = Files.walk(Paths.get(INSTANCES_DIR, "engines"))) {
            paths.filter(Files::isRegularFile)
                    .forEach(path -> {
                        try {
                            JsonNode instance = mapper.readTree(path.toFile());
                            String name = instance.get("name").asText();
                            String uuid = instance.get("uuid").asText();

                            assertFalse("Name should not be empty for " + path, name.isEmpty());
                            assertFalse("UUID should not be empty for " + path, uuid.isEmpty());
                            assertTrue("UUID should start with 'engine:' for " + path, uuid.startsWith("engine:"));

                            // Specific content checks, e.g., horsepower > 0
                            if (instance.has("horsepower")) {
                                int horsepower = instance.get("horsepower").asInt();
                                assertTrue("Horsepower should be positive for " + path, horsepower > 0);
                            }

                            // Uniqueness check across all engines
                            // This would require loading all instances, done in separate test
                        } catch (IOException e) {
                            fail("Error processing " + path + ": " + e.getMessage());
                        }
                    });
        }
    }

    @Test
    public void testContentValidationForCars() throws IOException {
        try (Stream<Path> paths = Files.walk(Paths.get(INSTANCES_DIR, "cars"))) {
            paths.filter(Files::isRegularFile)
                    .forEach(path -> {
                        try {
                            JsonNode instance = mapper.readTree(path.toFile());
                            String name = instance.get("name").asText();
                            String uuid = instance.get("uuid").asText();

                            assertFalse("Name should not be empty for " + path, name.isEmpty());
                            assertFalse("UUID should not be empty for " + path, uuid.isEmpty());
                            assertTrue("UUID should start with 'car:' for " + path, uuid.startsWith("car:"));

                            // Check engineRelationships
                            JsonNode relationships = instance.get("engineRelationships");
                            assertTrue("engineRelationships should be an array for " + path, relationships.isArray());
                            for (JsonNode rel : relationships) {
                                assertTrue("engineUuid should be present in relationship for " + path, rel.has("engineUuid"));
                                assertTrue("validFrom should be present in relationship for " + path, rel.has("validFrom"));
                            }

                            // maxSpeed > 0
                            int maxSpeed = instance.get("maxSpeed").asInt();
                            assertTrue("maxSpeed should be positive for " + path, maxSpeed > 0);
                        } catch (IOException e) {
                            fail("Error processing " + path + ": " + e.getMessage());
                        }
                    });
        }
    }

    @Test
    public void testUniquenessOfUUIDs() throws IOException {
        Set<String> uuids = new HashSet<>();

        try (Stream<Path> paths = Files.walk(Paths.get(INSTANCES_DIR))) {
            paths.filter(Files::isRegularFile)
                    .forEach(path -> {
                        try {
                            JsonNode instance = mapper.readTree(path.toFile());
                            String uuid = instance.get("uuid").asText();
                            assertTrue("Duplicate UUID found: " + uuid + " in " + path, uuids.add(uuid));
                        } catch (IOException e) {
                            fail("Error processing " + path + ": " + e.getMessage());
                        }
                    });
        }
    }

    @Test
    public void testRelationshipIntegrity() throws IOException {
        Set<String> engineUuids = getAllUUIDs("engines");

        try (Stream<Path> paths = Files.walk(Paths.get(INSTANCES_DIR, "cars"))) {
            paths.filter(Files::isRegularFile)
                    .forEach(path -> {
                        try {
                            JsonNode instance = mapper.readTree(path.toFile());
                            JsonNode relationships = instance.get("engineRelationships");
                            for (JsonNode rel : relationships) {
                                String engineUuid = rel.get("engineUuid").asText();
                                assertTrue("Invalid engineUuid in " + path + ": " + engineUuid, engineUuids.contains(engineUuid));
                            }
                        } catch (IOException e) {
                            fail("Error processing " + path + ": " + e.getMessage());
                        }
                    });
        }
    }

    private Set<String> getAllUUIDs(String subDir) throws IOException {
        Set<String> uuids = new HashSet<>();
        try (Stream<Path> paths = Files.walk(Paths.get(INSTANCES_DIR, subDir))) {
            paths.filter(Files::isRegularFile)
                    .forEach(path -> {
                        try {
                            JsonNode instance = mapper.readTree(path.toFile());
                            uuids.add(instance.get("uuid").asText());
                        } catch (IOException e) {
                            fail("Error processing " + path + ": " + e.getMessage());
                        }
                    });
        }
        return uuids;
    }

    @Test
    public void testValidDateFormatsInRelationships() throws IOException {
        try (Stream<Path> paths = Files.walk(Paths.get(INSTANCES_DIR, "cars"))) {
            paths.filter(Files::isRegularFile)
                    .forEach(path -> {
                        try {
                            JsonNode instance = mapper.readTree(path.toFile());
                            JsonNode relationships = instance.get("engineRelationships");
                            for (JsonNode rel : relationships) {
                                String validFrom = rel.get("validFrom").asText();
                                assertTrue("Invalid validFrom format in " + path + ": " + validFrom, isValidDateTime(validFrom));

                                if (rel.has("validTo")) {
                                    String validTo = rel.get("validTo").asText();
                                    assertTrue("Invalid validTo format in " + path + ": " + validTo, isValidDateTime(validTo));
                                }
                            }
                        } catch (IOException e) {
                            fail("Error processing " + path + ": " + e.getMessage());
                        }
                    });
        }
    }

    private boolean isValidDateTime(String dateStr) {
        try {
            java.time.ZonedDateTime.parse(dateStr);
            return true;
        } catch (java.time.format.DateTimeParseException e) {
            return false;
        }
    }

    @Test
    public void testNegativeSchemaValidation() {
        // Create invalid mock instance (e.g., missing required field)
        String invalidJson = "{\"name\": \"invalid-car\", \"uuid\": \"car:invalid\", \"model\": \"InvalidModel\", \"engineRelationships\": []}";
        try {
            JsonNode instance = mapper.readTree(invalidJson);
            JsonSchema schema = factory.getSchema(mapper.readTree(new File("src/main/resources/schemas/cars/car-sedan.json")));
            Set<ValidationMessage> errors = schema.validate(instance);
            assertFalse("Invalid instance should have errors", errors.isEmpty());
        } catch (IOException e) {
            fail("Error in negative validation test: " + e.getMessage());
        }
    }

    // Additional tests for EngineService can remain or be updated as needed
}
