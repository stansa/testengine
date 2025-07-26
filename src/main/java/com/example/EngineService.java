package com.example;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.generated.json.Aggregator;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class EngineService {
    private static final Map<String, Object> UUID_TO_TYPED_INSTANCE = Aggregator.UUID_TO_TYPED_INSTANCE;
    private static final Map<String, String> UUID_TO_SCHEMA_TYPE = Aggregator.UUID_TO_SCHEMA_TYPE;
    private static final Map<String, Set<String>> CAR_UUID_TO_ENGINE_UUIDS = Aggregator.CAR_UUID_TO_ENGINE_UUIDS;
    private static final ObjectMapper mapper = new ObjectMapper();

    public static <T> Optional<T> getEngineForCar(String carUuid, Class<T> engineClass) {
        Set<String> engineUuids = CAR_UUID_TO_ENGINE_UUIDS.getOrDefault(carUuid, new HashSet<>());
        String schemaType = engineClass.getSimpleName().toLowerCase().replace("engine", "");
        Set<String> matchingEngineUuids = engineUuids.stream()
                .filter(uuid -> UUID_TO_SCHEMA_TYPE.getOrDefault(uuid, "").equals(schemaType))
                .collect(Collectors.toSet());

        if (matchingEngineUuids.size() > 1) {
            System.err.println("Warning: Multiple " + schemaType + " instances found for Car UUID: " + carUuid + "; selecting first");
        }

        return matchingEngineUuids.stream()
                .map(uuid -> (T) UUID_TO_TYPED_INSTANCE.get(uuid))
                .findFirst();
    }
}