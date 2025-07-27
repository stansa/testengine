package com.example;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.github.classgraph.ClassGraph;
import io.github.classgraph.ScanResult;

public class JsonToJavaGenerator {

    private static final ObjectMapper mapper = new ObjectMapper();
    private static final String TARGET_PACKAGE = "com.example.generated.json";

    public static void main(String[] args) throws Exception {
        Path targetDir = args.length > 0 ? Paths.get(args[0]) : Paths.get("src/main/java/com/example/generated/json");
        Files.createDirectories(targetDir);

        List<String> jsonPaths = new ArrayList<>();
        try (ScanResult scanResult = new ClassGraph().acceptPaths("/schemas", "/instances").enableAllInfo().scan()) {
            scanResult.getResourcesWithExtension("json").forEach(resource -> {
                jsonPaths.add(resource.getPath());
            });
        }

        List<String> generatedClasses = new ArrayList<>();

        for (String jsonPath : jsonPaths) {
            String className = generateJavaClass(jsonPath, targetDir);
            if (className != null) {
                generatedClasses.add(className);
            }
        }

        generateAggregator(targetDir, generatedClasses);

        System.out.println("Generated " + jsonPaths.size() + " Java classes with embedded JSON in " + targetDir + ". Aggregator generated.");
    }

    private static String generateJavaClass(String jsonPath, Path targetDir) throws Exception {
        // Sanitize class name from path
        String className = jsonPath.replaceAll("[/.-]", "_").replaceAll("^_", "");
        className = Character.toUpperCase(className.charAt(0)) + className.substring(1);

        // Read JSON content
        try (InputStream is = JsonToJavaGenerator.class.getClassLoader().getResourceAsStream(jsonPath)) {
            if (is == null) {
                throw new IOException("Resource not found: " + jsonPath);
            }
            String jsonContent = new Scanner(is, StandardCharsets.UTF_8.name()).useDelimiter("\\A").next();
            jsonContent = jsonContent.replace("\"", "\\\"").replace("\n", "\\n");

            // Parse JSON to extract fields for POJO constants
            JsonNode instance = mapper.readTree(jsonContent);
            StringBuilder fieldsCode = new StringBuilder();
            instance.fields().forEachRemaining(field -> {
                String fieldName = field.getKey().toUpperCase().replace("-", "_");
                JsonNode value = field.getValue();
                if (value.isTextual()) {
                    String strValue = value.asText().replace("\"", "\\\"");
                    fieldsCode.append("    public static final String ").append(fieldName).append(" = \"").append(strValue).append("\";\n");
                } else if (value.isNumber()) {
                    fieldsCode.append("    public static final ").append(value.isInt() ? "int" : "double").append(" ").append(fieldName).append(" = ").append(value.asText()).append(";\n");
                } else if (value.isBoolean()) {
                    fieldsCode.append("    public static final boolean ").append(fieldName).append(" = ").append(value.asBoolean()).append(";\n");
                } else if (value.isArray()) {
                    String arrayType = getArrayType(value);
                    StringBuilder arrayValue = new StringBuilder();
                    arrayValue.append("{ ");
                    int index = 0;
                    for (JsonNode elem : value) {
                        if (elem.isObject()) {
                            // Generate sub-class for object in array
                            String subClassName = fieldName + "_" + index;
                            String subClassCode = generateSubClassCode(subClassName, elem);
                            fieldsCode.append(subClassCode);
                            arrayValue.append(subClassName).append(".class, ");
                        } else if (elem.isTextual()) {
                            arrayValue.append("\"").append(elem.asText().replace("\"", "\\\"")).append("\", ");
                        } else {
                            arrayValue.append(elem.asText()).append(", ");
                        }
                        index++;
                    }
                    if (arrayValue.length() > 2) {
                        arrayValue.setLength(arrayValue.length() - 2);
                    }
                    arrayValue.append(" }");
                    fieldsCode.append("    public static final ").append(arrayType).append("[] ").append(fieldName).append(" = ").append(arrayValue).append(";\n");
                } else if (value.isObject()) {
                    // Generate sub-class for object field
                    String subClassName = fieldName;
                    String subClassCode = generateSubClassCode(subClassName, value);
                    fieldsCode.append(subClassCode);
                    fieldsCode.append("    public static final Class<?> ").append(fieldName).append(" = ").append(subClassName).append(".class;\n");
                }
            });

            // Generate Java code
            StringBuilder javaCode = new StringBuilder();
            javaCode.append("package ").append(TARGET_PACKAGE).append(";\n\n");
            javaCode.append("public class ").append(className).append(" {\n");
            javaCode.append("    public static final String JSON = \"").append(jsonContent).append("\";\n\n");
            javaCode.append(fieldsCode);
            javaCode.append("}\n");

            // Write to file
            Path javaFile = targetDir.resolve(className + ".java");
            Files.writeString(javaFile, javaCode.toString());
            System.out.println("Generated: " + javaFile);
            return className;
        } catch (IOException e) {
            System.err.println("Failed to generate class for " + jsonPath + ": " + e.getMessage());
            return null;
        }
    }

    private static String generateSubClassCode(String subClassName, JsonNode objectNode) {
        StringBuilder subCode = new StringBuilder();
        subCode.append("    public static class ").append(subClassName).append(" {\n");
        objectNode.fields().forEachRemaining(field -> {
            String fieldName = field.getKey().toUpperCase().replace("-", "_");
            JsonNode value = field.getValue();
            if (value.isTextual()) {
                String strValue = value.asText().replace("\"", "\\\"");
                subCode.append("        public static final String ").append(fieldName).append(" = \"").append(strValue).append("\";\n");
            } else if (value.isNumber()) {
                subCode.append("        public static final ").append(value.isInt() ? "int" : "double").append(" ").append(fieldName).append(" = ").append(value.asText()).append(";\n");
            } else if (value.isBoolean()) {
                subCode.append("        public static final boolean ").append(fieldName).append(" = ").append(value.asBoolean()).append(";\n");
            } else if (value.isArray()) {
                String arrayType = getArrayType(value);
                StringBuilder arrayValue = new StringBuilder();
                arrayValue.append("{ ");
                for (JsonNode elem : value) {
                    if (elem.isTextual()) {
                        arrayValue.append("\"").append(elem.asText().replace("\"", "\\\"")).append("\", ");
                    } else {
                        arrayValue.append(elem.asText()).append(", ");
                    }
                }
                if (arrayValue.length() > 2) {
                    arrayValue.setLength(arrayValue.length() - 2);
                }
                arrayValue.append(" }");
                subCode.append("        public static final ").append(arrayType).append("[] ").append(fieldName).append(" = ").append(arrayValue).append(";\n");
            } else if (value.isObject()) {
                String objValue = value.toString().replace("\"", "\\\"").replace("\n", "\\n");
                subCode.append("        public static final String ").append(fieldName).append(" = \"").append(objValue).append("\";\n");
            }
        });
        subCode.append("    }\n");
        return subCode.toString();
    }

    private static String getArrayType(JsonNode arrayNode) {
        if (arrayNode.size() > 0) {
            JsonNode first = arrayNode.get(0);
            if (first.isTextual()) return "String";
            if (first.isInt()) return "int";
            if (first.isDouble()) return "double";
            if (first.isBoolean()) return "boolean";
        }
        return "Object";
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
