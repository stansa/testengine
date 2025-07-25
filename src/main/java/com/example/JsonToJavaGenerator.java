import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import io.github.classgraph.ClassGraph;
import io.github.classgraph.ScanResult;

public class JsonToJavaGenerator {

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

        for (String jsonPath : jsonPaths) {
            generateJavaClass(jsonPath, targetDir);
        }

        System.out.println("Generated " + jsonPaths.size() + " Java classes with embedded JSON in " + targetDir);
    }

    private static void generateJavaClass(String jsonPath, Path targetDir) throws Exception {
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

            // Generate Java code
            StringBuilder javaCode = new StringBuilder();
            javaCode.append("package ").append(TARGET_PACKAGE).append(";\n\n");
            javaCode.append("public class ").append(className).append(" {\n");
            javaCode.append("    public static final String JSON = \"").append(jsonContent).append("\";\n");
            javaCode.append("}\n");

            // Write to file
            Path javaFile = targetDir.resolve(className + ".java");
            Files.writeString(javaFile, javaCode.toString());
            System.out.println("Generated: " + javaFile);
        }
    }
}