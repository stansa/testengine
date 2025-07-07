package com.example.engine;

import java.util.Map;

public class SchemaTypeMapping {
    public static final Map<Class<?>, String> CLASS_TO_SCHEMA_TYPE = Map.of(
            com.example.engine.generated.engines.EngineGas.class, "engine-gas",
            com.example.engine.generated.engines.EngineElectric.class, "engine-electric",
            com.example.engine.generated.engines.EngineHybrid.class, "engine-hybrid"
    );
}
