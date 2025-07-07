package com.example;

import com.example.engine.EngineValidation;
import com.example.engine.EngineService;
import com.example.engine.generated.engines.EngineGas;
import com.example.engine.generated.engines.EngineElectric;
import com.example.engine.generated.engines.EngineHybrid;

import java.io.File;
import java.util.Optional;

public class Main {
    public static void main(String[] args) {
        // Check for "validate" argument
        boolean validateOnly = args.length > 0 && args[0].equals("validate");

        if (validateOnly) {
            // Step 1: Validate JSON schemas and instances
            try {
                File schemasDir = new File("src/main/resources/schemas");
                File instancesDir = new File("src/main/resources/instances");
                File relationshipsFile = new File("src/main/resources/generated/relationships.json");
                EngineValidation.validate(schemasDir, instancesDir, relationshipsFile);
                System.out.println("Validation succeeded. Run 'mvn generate-sources' to generate classes, then 'mvn compile' and 'mvn package'.");
            } catch (Exception e) {
                System.err.println("Validation failed: " + e.getMessage());
                System.exit(1);
            }
        } else {
            // Default behavior: Call getEngineForCar methods
            String sedanCarUuid = "abcdef12-3456-7890-abcd-ef1234567890";
            Optional<EngineGas> gasEngineOpt = EngineService.getEngineForCar(sedanCarUuid, EngineGas.class);
            gasEngineOpt.ifPresent(engine -> {
                System.out.println("Gas Engine (Sedan): " + engine.horsepower + ", " +
                        engine.fuelEfficiency + ", " + engine.fuelTypes);
            });

            Optional<EngineElectric> electricEngineOpt = EngineService.getEngineForCar(sedanCarUuid, EngineElectric.class);
            electricEngineOpt.ifPresent(engine -> {
                System.out.println("Electric Engine (Sedan): " + engine.batteryCapacity + ", " +
                        engine.rangeMiles + ", " + engine.chargingTypes);
            });

            Optional<EngineHybrid> hybridEngineOpt = EngineService.getEngineForCar(sedanCarUuid, EngineHybrid.class);
            hybridEngineOpt.ifPresent(engine -> {
                System.out.println("Hybrid Engine (Sedan): " + engine.horsepower + ", " +
                        engine.batteryCapacity + ", " + engine.fuelEfficiency);
            });

            String suvCarUuid = "789abcde-f123-4567-89ab-cdef12345678";
            Optional<EngineGas> suvGasEngineOpt = EngineService.getEngineForCar(suvCarUuid, EngineGas.class);
            suvGasEngineOpt.ifPresent(engine -> {
                System.out.println("Gas Engine (SUV): " + engine.horsepower + ", " +
                        engine.fuelEfficiency + ", " + engine.fuelTypes);
            });

            Optional<EngineElectric> suvElectricEngineOpt = EngineService.getEngineForCar(suvCarUuid, EngineElectric.class);
            suvElectricEngineOpt.ifPresent(engine -> {
                System.out.println("Electric Engine (SUV): " + engine.batteryCapacity + ", " +
                        engine.rangeMiles + ", " + engine.chargingTypes);
            });

            Optional<EngineHybrid> suvHybridEngineOpt = EngineService.getEngineForCar(suvCarUuid, EngineHybrid.class);
            suvHybridEngineOpt.ifPresent(engine -> {
                System.out.println("Hybrid Engine (SUV): " + engine.horsepower + ", " +
                        engine.batteryCapacity + ", " + engine.fuelEfficiency);
            });
        }
    }
}