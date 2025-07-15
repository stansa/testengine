
        package com.example.engine;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.StringJoiner;
import java.util.stream.StreamSupport;

public class GenerateVisuals {
    private static final ObjectMapper mapper = new ObjectMapper();

    public static void main(String[] args) throws Exception {
        generateMermaid();
        generateD3Data();
        generateGraphviz();
    }

    private static void generateMermaid() throws Exception {
        StringBuilder mermaid = new StringBuilder("graph TD\n");

        // Visualize schemas
        mermaid.append("subgraph Schemas\n");
        visualizeSchemas(mermaid, Paths.get("src/main/resources/schemas/engines"));
        visualizeSchemas(mermaid, Paths.get("src/main/resources/schemas/cars"));
        mermaid.append("end\n");

        // Visualize instances
        mermaid.append("subgraph Instances\n");
        visualizeInstances(mermaid, Paths.get("src/main/resources/instances/engines"));
        visualizeInstances(mermaid, Paths.get("src/main/resources/instances/cars"));
        mermaid.append("end\n");

        // Visualize relationships
        mermaid.append("subgraph Relationships\n");
        JsonNode relationships = mapper.readTree(Paths.get("src/main/resources/generated/relationships.json").toFile());
        relationships.fields().forEachRemaining(field -> {
            String carUuid = field.getKey();
            JsonNode engines = field.getValue();
            StreamSupport.stream(engines.spliterator(), false).forEach(engineUuid -> {
                mermaid.append(carUuid).append(" --> ").append(engineUuid.asText()).append("\n");
            });
        });
        mermaid.append("end\n");

        try (FileWriter writer = new FileWriter("README.md", true)) {
            writer.append("\n### Visualizations\n```mermaid\n").append(mermaid).append("\n```\n");
        }
        System.out.println("Generated Mermaid in README.md");
    }

    private static void visualizeSchemas(StringBuilder mermaid, Path dir) throws IOException {
        Files.walk(dir)
                .filter(path -> path.toString().endsWith(".json"))
                .forEach(path -> {
                    try {
                        JsonNode schema = mapper.readTree(path.toFile());
                        JsonNode properties = schema.get("properties");
                        if (properties != null) {
                            String schemaName = path.getFileName().toString().replace(".json", "");
                            mermaid.append(schemaName).append("[\"").append(schemaName).append("\"]").append("\n");
                            properties.fields().forEachRemaining(field -> {
                                String propName = field.getKey();
                                JsonNode prop = field.getValue();
                                String type = prop.get("type").asText();
                                String details = "";
                                if (prop.has("pattern")) {
                                    details += " (pattern: " + prop.get("pattern").asText() + ")";
                                } else if (prop.has("minimum")) {
                                    details += " (min: " + prop.get("minimum").asText() + ", max: " + prop.get("maximum").asText() + ")";
                                }
                                mermaid.append(schemaName).append(" -- ").append(propName).append(" [").append(type).append(details).append("]\n");
                            });
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
    }

    private static void visualizeInstances(StringBuilder mermaid, Path dir) throws IOException {
        Files.walk(dir)
                .filter(path -> path.toString().endsWith(".json"))
                .forEach(path -> {
                    try {
                        JsonNode instance = mapper.readTree(path.toFile());
                        String uuid = instance.get("uuid").asText();
                        mermaid.append(uuid).append("[\"").append(uuid).append("\"]").append("\n");
                        instance.fields().forEachRemaining(field -> {
                            String key = field.getKey();
                            JsonNode value = field.getValue();
                            String valStr = value.isArray() ? value.toString() : value.asText();
                            mermaid.append(uuid).append(" -- ").append(key).append(" = ").append(valStr).append("\n");
                        });
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
    }

    private static void generateD3Data() throws IOException {
        // Generate D3 data for schemas, instances, relationships
        StringJoiner data = new StringJoiner(",");
        data.add("\"schemas\": " + mapper.writeValueAsString(getJsonTree("schemas")));
        data.add("\"instances\": " + mapper.writeValueAsString(getJsonTree("instances")));
        data.add("\"relationships\": " + Files.readString(Paths.get("src/main/resources/generated/relationships.json")));
        try (FileWriter writer = new FileWriter("visuals/d3-data.json")) {
            writer.write("{" + data + "}");
        }
        System.out.println("Generated D3 data in visuals/d3-data.json");
    }

    private static JsonNode getJsonTree(String dirName) throws IOException {
        JsonNode root = mapper.createObjectNode();
        Path dir = Paths.get("src/main/resources/" + dirName);
        Files.walk(dir)
                .filter(path -> path.toString().endsWith(".json"))
                .forEach(path -> {
                    try {
                        JsonNode node = mapper.readTree(path.toFile());
                        ((ObjectNode) root).put(path.getFileName().toString(), node);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
        return root;
    }

    private static void generateGraphviz() throws Exception {
        StringBuilder dot = new StringBuilder("digraph G {\n");

        // Visualize schemas
        dot.append("subgraph cluster_schemas {\nlabel=\"Schemas\";\n");
        visualizeSchemasGraphviz(dot, Paths.get("src/main/resources/schemas/engines"));
        visualizeSchemasGraphviz(dot, Paths.get("src/main/resources/schemas/cars"));
        dot.append("}\n");

        // Visualize instances
        dot.append("subgraph cluster_instances {\nlabel=\"Instances\";\n");
        visualizeInstancesGraphviz(dot, Paths.get("src/main/resources/instances/engines"));
        visualizeInstancesGraphviz(dot, Paths.get("src/main/resources/instances/cars"));
        dot.append("}\n");

        // Visualize relationships
        dot.append("subgraph cluster_relationships {\nlabel=\"Relationships\";\n");
        JsonNode relationships = mapper.readTree(Paths.get("src/main/resources/generated/relationships.json").toFile());
        relationships.fields().forEachRemaining(field -> {
            String carUuid = field.getKey();
            JsonNode engines = field.getValue();
            StreamSupport.stream(engines.spliterator(), false).forEach(engineUuid -> {
                dot.append(carUuid).append(" -> ").append(engineUuid.asText()).append(";\n");
            });
        });
        dot.append("}\n");

        dot.append("}\n");

        try (FileWriter writer = new FileWriter("visuals/relationships.dot")) {
            writer.write(dot.toString());
        }
        System.out.println("Generated Graphviz DOT in visuals/relationships.dot. Run 'dot -Tsvg visuals/relationships.dot -o visuals/relationships.svg' to render.");
    }

    private static void visualizeSchemasGraphviz(StringBuilder dot, Path dir) throws IOException {
        Files.walk(dir)
                .filter(path -> path.toString().endsWith(".json"))
                .forEach(path -> {
                    try {
                        JsonNode schema = mapper.readTree(path.toFile());
                        JsonNode properties = schema.get("properties");
                        if (properties != null) {
                            String schemaName = path.getFileName().toString().replace(".json", "");
                            dot.append(schemaName).append(" [label=\"").append(schemaName).append("\"];\n");
                            properties.fields().forEachRemaining(field -> {
                                String propName = field.getKey();
                                JsonNode prop = field.getValue();
                                String type = prop.get("type").asText();
                                String details = "";
                                if (prop.has("pattern")) {
                                    details += " (pattern: " + prop.get("pattern").asText() + ")";
                                } else if (prop.has("minimum")) {
                                    details += " (min: " + prop.get("minimum").asText() + ", max: " + prop.get("maximum").asText() + ")";
                                }
                                dot.append(schemaName).append(" -> ").append(propName).append(" [label=\"").append(type).append(details).append("\"];\n");
                            });
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
    }

    private static void visualizeInstancesGraphviz(StringBuilder dot, Path dir) throws IOException {
        Files.walk(dir)
                .filter(path -> path.toString().endsWith(".json"))
                .forEach(path -> {
                    try {
                        JsonNode instance = mapper.readTree(path.toFile());
                        String uuid = instance.get("uuid").asText();
                        dot.append(uuid).append(" [label=\"").append(uuid).append("\"];\n");
                        instance.fields().forEachRemaining(field -> {
                            String key = field.getKey();
                            JsonNode value = field.getValue();
                            String valStr = value.isArray() ? value.toString() : value.asText();
                            dot.append(uuid).append(" -> ").append(key).append(" [label=\"").append(valStr).append("\"];\n");
                        });
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
    }
}
