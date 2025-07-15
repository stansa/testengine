 package com.example.engine;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class EngineValidation {
    public static void validate(File schemasDir, File instancesDir, File relationshipsFile) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7);
        Map<String, Set<String>> carUuidToEngineUuids = new HashMap<>();
        Map<String, String> uuidToSchemaType = new HashMap<>();

        // Validate instances and build relationships
        for (String subDir : new String[]{"engines", "cars"}) {
            Path subDirPath = instancesDir.toPath().resolve(subDir);
            if (Files.exists(subDirPath)) {
                Files.walk(subDirPath)
                        .filter(path -> path.toString().endsWith(".json"))
                        .forEach(path -> {
                            try {
                                String jsonContent = Files.readString(path);
                                JsonNode jsonNode = mapper.readTree(jsonContent);
                                String name = jsonNode.get("name") != null ? jsonNode.get("name").asText() : null;
                                String uuid = jsonNode.get("uuid") != null ? jsonNode.get("uuid").asText() : null;
                                String fileName = path.getFileName().toString();
                                String schemaType = fileName.substring(0, fileName.lastIndexOf('-'));

                                if (name == null || uuid == null) {
                                    throw new Exception("Invalid instance: " + fileName + " - Missing name or uuid");
                                }

                                // Validate against schema
                                Path schemaPath = schemasDir.toPath().resolve(subDir).resolve(schemaType + ".json");
                                if (!Files.exists(schemaPath)) {
                                    throw new Exception("Schema not found for: " + schemaType + " at " + fileName);
                                }
                                JsonSchema schema = factory.getSchema(Files.newInputStream(schemaPath));
                                Set<ValidationMessage> errors = schema.validate(jsonNode);
                                if (!errors.isEmpty()) {
                                    StringBuilder errorMsg = new StringBuilder("Validation errors for: " + fileName + "\n");
                                    errors.forEach(error -> errorMsg.append("  - ").append(error.getMessage()).append("\n"));
                                    throw new Exception(errorMsg.toString());
                                }

                                // Store schema type for relationship validation
                                uuidToSchemaType.put(uuid, schemaType);

                                // Collect relationships from Car instances only (updated requirement)
                                if (subDir.equals("cars")) {
                                    JsonNode engineRelationshipsNode = jsonNode.get("engineRelationships");
                                    if (engineRelationshipsNode != null && engineRelationshipsNode.isArray()) {
                                        engineRelationshipsNode.forEach(node -> {
                                            String engineUuid = node.get("engineUuid").asText();
                                            carUuidToEngineUuids.computeIfAbsent(uuid, k -> new HashSet<>()).add(engineUuid);
                                        });
                                    }
                                }
                            } catch (Exception e) {
                                throw new RuntimeException("Failed to process: " + path, e);
                            }
                        });
            }
        }

        // Generate relationships.json
        Files.createDirectories(relationshipsFile.getParentFile().toPath());
        mapper.writeValue(relationshipsFile, carUuidToEngineUuids);
        System.out.println("Generated relationships file: " + relationshipsFile);
    }
}
