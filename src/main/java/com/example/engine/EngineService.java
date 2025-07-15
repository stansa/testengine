
        package com.example.engine;

import com.example.engine.generated.engines.EngineGas;
import com.example.engine.generated.engines.EngineElectric;
import com.example.engine.generated.engines.EngineHybrid;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.classgraph.ClassGraph;
import io.github.classgraph.ScanResult;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class EngineService {
    private static final Map<String, String> nameToJson = new ConcurrentHashMap<>();
    private static final Map<String, String> uuidToJson = new ConcurrentHashMap<>();
    private static final Map<String, String> nameToSchemaType = new ConcurrentHashMap<>();
    private static final Map<String, Set<String>> carUuidToEngineUuids;
    private static final ObjectMapper objectMapper = new ObjectMapper();

    static {
        // Load relationships from generated file
        try (InputStream is = EngineService.class.getClassLoader().getResourceAsStream("generated/relationships.json")) {
            if (is == null) {
                throw new RuntimeException("Relationships file not found");
            }
            carUuidToEngineUuids = objectMapper.readValue(is,
                    objectMapper.getTypeFactory().constructMapType(Map.class, String.class, Set.class));
        } catch (IOException e) {
            throw new RuntimeException("Failed to load relationships", e);
        }

        // Load JSON documents from engines subfolder
        try (ScanResult scanResult = new ClassGraph().acceptPaths("/instances/engines").enableAllInfo().scan()) {
            scanResult.getResourcesWithExtension("json").forEach(resource -> {
                try (InputStream is = resource.open()) {
                    String jsonContent = new String(is.readAllBytes());
                    JsonNode jsonNode = objectMapper.readTree(jsonContent);
                    String name = jsonNode.get("name") != null ? jsonNode.get("name").asText() : null;
                    String uuid = jsonNode.get("uuid") != null ? jsonNode.get("uuid").asText() : null;
                    String filePath = resource.getPath();
                    String schemaType = filePath.substring(filePath.lastIndexOf('/') + 1, filePath.lastIndexOf('-'));

                    if (name == null || uuid == null) {
                        System.err.println("Invalid JSON document: " + filePath + " - Missing name or uuid");
                        return;
                    }

                    nameToJson.put(name, jsonContent);
                    uuidToJson.put(uuid, jsonContent);
                    nameToSchemaType.put(name, schemaType);
                } catch (IOException e) {
                    System.err.println("Failed to load JSON document: " + resource.getPath());
                }
            });
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize EngineService", e);
        }
    }

    public static <T> Optional<T> getEngineForCar(String carUuid, Class<T> engineClass) {
        String schemaType = SchemaTypeMapping.CLASS_TO_SCHEMA_TYPE.get(engineClass);
        if (schemaType == null || !schemaType.startsWith("engine-")) {
            throw new IllegalArgumentException("Unknown or invalid engine class: " + engineClass.getName());
        }
        String engineType = schemaType.substring("engine-".length());
        Set<String> engineUuids = carUuidToEngineUuids.getOrDefault(carUuid, Set.of()).stream()
                .filter(uuid -> nameToSchemaType.getOrDefault(
                                nameToJson.entrySet().stream()
                                        .filter(e -> e.getValue().equals(uuidToJson.get(uuid)))
                                        .map(Map.Entry::getKey)
                                        .findFirst()
                                        .orElse(""), "")
                        .equals(schemaType))
                .collect(Collectors.toSet());

        if (engineUuids.size() > 1) {
            System.err.println("Warning: Multiple " + schemaType + " instances found for Car UUID: " + carUuid + "; selecting first");
        }

        return engineUuids.stream()
                .map(uuid -> getJsonDocumentByUuid(uuid))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(json -> createEngineInstance(json, schemaType, engineClass))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .findFirst();
    }

    private static Optional<String> getJsonDocumentByUuid(String uuid) {
        return Optional.ofNullable(uuidToJson.get(uuid));
    }

    private static <T> Optional<T> createEngineInstance(String json, String schemaType, Class<T> engineClass) {
        try {
            Class<?> targetClass = engineClass;
            if (targetClass == null) {
                for (Map.Entry<Class<?>, String> entry : SchemaTypeMapping.CLASS_TO_SCHEMA_TYPE.entrySet()) {
                    if (entry.getValue().equals(schemaType)) {
                        targetClass = entry.getKey();
                        break;
                    }
                }
            }
            if (targetClass == null) {
                throw new IllegalArgumentException("No class found for schema type: " + schemaType);
            }

            switch (schemaType) {
                case "engine-gas":
                    if (targetClass.equals(EngineGas.class)) {
                        return Optional.of(targetClass.cast(objectMapper.readValue(json, EngineGas.class)));
                    }
                    break;
                case "engine-electric":
                    if (targetClass.equals(EngineElectric.class)) {
                        return Optional.of(targetClass.cast(objectMapper.readValue(json, EngineElectric.class)));
                    }
                    break;
                case "engine-hybrid":
                    if (targetClass.equals(EngineHybrid.class)) {
                        return Optional.of(targetClass.cast(objectMapper.readValue(json, EngineHybrid.class)));
                    }
                    break;
                default:
                    throw new IllegalArgumentException("Unknown schema type: " + schemaType);
            }
            return Optional.empty();
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse JSON for schema type " + schemaType, e);
        }
    }
}
