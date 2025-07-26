package com.example;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class JsonToJavaGenerator {

    private static final ObjectMapper mapper = new ObjectMapper();
    private static final String TARGET_PACKAGE = "com.example.generated.json";

    public static void main(String[] args) throws Exception {
        Path targetDir = args.length > 0 ? Paths.get(args[0]) : Paths.get("src/main/java/com/example/generated/json");
        Files.createDirectories(targetDir);

        Path sourceDir = Paths.get("src/main/resources"); // Local directory for schemas and instances
        List<Path> jsonFiles = new ArrayList<>();

        // Walk through schemas and instances subdirectories
        Stream.of("schemas", "instances").forEach(subDir -> {
            Path dir = sourceDir.resolve(subDir);
            if (Files.exists(dir)) {
                try (Stream<Path> paths = Files.walk(dir)) {
                    paths.filter(Files::isRegularFile)
                            .filter(path -> path.toString().endsWith(".json"))
                            .forEach(jsonFiles::add);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        List<String> generatedClasses = new ArrayList<>();

        for (Path jsonFile : jsonFiles) {
            String className = generateJavaClass(jsonFile, targetDir, sourceDir);
            if (className != null) {
                generatedClasses.add(className);
            }
        }

        generateAggregator(targetDir, generatedClasses);

        System.out.println("Generated " + jsonFiles.size() + " Java classes with embedded JSON in " + targetDir + ". Aggregator generated.");
    }

    private static String generateJavaClass(Path jsonFile, Path targetDir, Path sourceDir) throws Exception {
        // Sanitize class name from relative path
        Path relativePath = sourceDir.relativize(jsonFile);
        String className = relativePath.toString().replaceAll("[/.-]", "_").replaceAll("^_", "");
        className = Character.toUpperCase(className.charAt(0)) + className.substring(1);

        // Read JSON content
        String jsonContent = Files.readString(jsonFile, StandardCharsets.UTF_8);
        jsonContent = jsonContent.replace("\"", "\\\"").replace("\n", "\\n");

        // Generate Java code
        StringBuilder javaCode = new StringBuilder();
        javaCode.append("package ").append(TARGET_PACKAGE).append(";\n\n");
        javaCode.append("public class ").append(className).append(" {\n");
        javaCode.append("    public static final String JSON = \"").append(jsonContent).append("\";\n");

        // If this is a car instance, add associated engines
        if (relativePath.toString().contains("/instances/cars/")) {
            JsonNode instance = mapper.readTree(Files.readString(jsonFile));
            JsonNode rels = instance.get("engineRelationships");
            if (rels != null && rels.isArray()) {
                for (JsonNode rel : rels) {
                    String engineUuid = rel.get("engineUuid").asText();
                    Path enginePath = sourceDir.resolve("instances/engines/" + engineUuid.replace(":", "-") + ".json");
                    if (Files.exists(enginePath)) {
                        String engineJson = Files.readString(enginePath, StandardCharsets.UTF_8);
                        engineJson = engineJson.replace("\"", "\\\"").replace("\n", "\\n");
                        String engineConstantName = "ENGINE_" + engineUuid.toUpperCase().replace(":", "_").replace("-", "_") + "_JSON";
                        javaCode.append("    public static final String ").append(engineConstantName).append(" = \"").append(engineJson).append("\";\n");
                    }
                }
            }
        }

        javaCode.append("}\n");

        // Write to file
        Path javaFile = targetDir.resolve(className + ".java");
        Files.writeString(javaFile, javaCode.toString());
        System.out.println("Generated: " + javaFile);
        return className;
    }

    private static void generateAggregator(Path targetDir, List<String> generatedClasses) throws Exception {
        StringBuilder javaCode = new StringBuilder();
        javaCode.append("package ").append(TARGET_PACKAGE).append(";\n\n");
        javaCode.append("import com.fasterxml.jackson.databind.JsonNode;\n");
        javaCode.append("import com.fasterxml.jackson.databind.ObjectMapper;\n");
        javaCode.append("import java.util.HashMap;\n");
        javaCode.append("import java.util.HashSet;\n");
        javaCode.append("import java.util.Map;\n");
        javaCode.append("import java.util.Set;\n");
        javaCode.append("import java.util.stream.StreamSupport;\n\n");
        javaCode.append("public class Aggregator {\n");
        javaCode.append("    public static final Map<String, JsonNode> UUID_TO_INSTANCE = new HashMap<>();\n");
        javaCode.append("    public static final Map<String, String> UUID_TO_SCHEMA_TYPE = new HashMap<>();\n");
        javaCode.append("    public static final Map<String, Set<String>> CAR_UUID_TO_ENGINE_UUIDS = new HashMap<>();\n");
        javaCode.append("    private static final ObjectMapper mapper = new ObjectMapper();\n\n");
        javaCode.append("    static {\n");

        // Load all generated classes
        for (String className : generatedClasses) {
            javaCode.append("        loadInstance(").append(className).append(".JSON);\n");
        }

        javaCode.append("    }\n\n");
        javaCode.append("    private static void loadInstance(String json) {\n");
        javaCode.append("        try {\n");
        javaCode.append("            JsonNode instance = mapper.readTree(json);\n");
        javaCode.append("            String uuid = instance.path(\"uuid\").asText();\n");
        javaCode.append("            if (uuid.isEmpty()) return;\n");
        javaCode.append("            UUID_TO_INSTANCE.put(uuid, instance);\n");
        javaCode.append("            String schemaType = uuid.split(\":\")[0];\n");
        javaCode.append("            UUID_TO_SCHEMA_TYPE.put(uuid, schemaType);\n");
        javaCode.append("            if (schemaType.equals(\"car\")) {\n");
        javaCode.append("                JsonNode rels = instance.path(\"engineRelationships\");\n");
        javaCode.append("                Set<String> engineUuids = new HashSet<>();\n");
        javaCode.append("                if (rels.isArray()) {\n");
        javaCode.append("                    StreamSupport.stream(rels.spliterator(), false).forEach(rel -> {\n");
        javaCode.append("                        engineUuids.add(rel.path(\"engineUuid\").asText());\n");
        javaCode.append("                    });\n");
        javaCode.append("                }\n");
        javaCode.append("                CAR_UUID_TO_ENGINE_UUIDS.put(uuid, engineUuids);\n");
        javaCode.append("            }\n");
        javaCode.append("        } catch (Exception e) {\n");
        javaCode.append("            e.printStackTrace();\n");
        javaCode.append("        }\n");
        javaCode.append("    }\n");
        javaCode.append("}\n");

        Path aggregatorFile = targetDir.resolve("Aggregator.java");
        Files.writeString(aggregatorFile, javaCode.toString());
        System.out.println("Generated Aggregator: " + aggregatorFile);
    }
}
