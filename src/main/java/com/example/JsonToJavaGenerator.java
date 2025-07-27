package com.example;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

import io.github.classgraph.ClassGraph;
import io.github.classgraph.ScanResult;

public class JsonToJavaGenerator {

    private static final ObjectMapper mapper = new ObjectMapper();
    private static final String TARGET_PACKAGE = "com.example.generated.json";

    public static void main(String[] args) throws Exception {
        Path targetDir = args.length > 0 ? Paths.get(args[0]) : Paths.get("src/main/java/com/example/generated/json");
        Files.createDirectories(targetDir);

        List<String> jsonPaths = new ArrayList<>();
        String propertiesPath = null;
        try (ScanResult scanResult = new ClassGraph().acceptPaths("/schemas", "/instances", "/version").enableAllInfo().scan()) {
            scanResult.getResourcesWithExtension("json").forEach(resource -> {
                jsonPaths.add(resource.getPath());
            });
            scanResult.getResourcesWithExtension("properties").forEach(resource -> {
                if (resource.getPath().endsWith("version.properties")) {
                    propertiesPath = resource.getPath();
                }
            });
        }

        List<String> generatedClasses = new ArrayList<>();

        for (String jsonPath : jsonPaths) {
            String className = generateJavaClass(jsonPath, targetDir);
            if (className != null) {
                generatedClasses.add(className);
            }
        }

        if (propertiesPath != null) {
            generatePropertiesClass(propertiesPath, targetDir);
            generatedClasses.add("VersionProperties");
        }

        generateAggregator(targetDir, generatedClasses);

        System.out.println("Generated " + jsonPaths.size() + " Java classes with embedded JSON and properties in " + targetDir + ". Aggregator generated.");
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
                    fieldsCode.append("    public static final ").append(arrayType).append("[] ").append(fieldName).append(" = ").append(arrayValue).append(";\n");
                } else if (value.isObject()) {
                    String objValue = value.toString().replace("\"", "\\\"").replace("\n", "\\n");
                    fieldsCode.append("    public static final String ").append(fieldName).append(" = \"").append(objValue).append("\";\n");
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
}
