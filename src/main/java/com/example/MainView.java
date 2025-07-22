package com.example.engine;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vaadin.flow.component.contextmenu.ContextMenu;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.data.provider.ListDataProvider;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.Route;
import io.github.classgraph.ClassGraph;
import io.github.classgraph.ScanResult;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Route("")
public class MainView extends VerticalLayout {
    private static final ObjectMapper mapper = new ObjectMapper();
    private final List<Map<String, String>> schemas = new ArrayList<>();
    private final List<Map<String, String>> instances = new ArrayList<>();
    private final Map<String, List<Map<String, String>>> carEngines = new HashMap<>();
    private final Map<String, String> instanceJson = new HashMap<>();
    private Map<String, String> selectedItem;
    private final Map<String, Map<String, String>> selectedEngine = new HashMap<>();

    public MainView() {
        loadData();

        // Create tabs
        Tab schemasTab = new Tab("Schemas");
        Tab instancesTab = new Tab("Instances");
        Tabs tabs = new Tabs(schemasTab, instancesTab);
        VerticalLayout schemasLayout = createSchemasTab();
        VerticalLayout instancesLayout = createInstancesTab();
        Map<Tab, VerticalLayout> tabsToLayouts = new HashMap<>();
        tabsToLayouts.put(schemasTab, schemasLayout);
        tabsToLayouts.put(instancesTab, instancesLayout);

        // Show selected tab content
        tabs.addSelectedChangeListener(event -> {
            removeAll();
            add(tabs, tabsToLayouts.get(event.getSelectedTab()));
        });

        add(tabs, schemasLayout);
    }

    private void loadData() {
        // Load schemas using ClassGraph
        try (ScanResult scanResult = new ClassGraph().acceptPaths("/schemas").enableAllInfo().scan()) {
            scanResult.getResourcesWithExtension("json").forEach(resource -> {
                try (InputStream is = resource.open()) {
                    JsonNode schema = mapper.readTree(is);
                    JsonNode properties = schema.get("properties");
                    if (properties != null) {
                        Map<String, String> schemaData = new HashMap<>();
                        schemaData.put("Schema Name", resource.getPath().substring("schemas/".length()));
                        StringBuilder props = new StringBuilder();
                        properties.fields().forEachRemaining(field -> {
                            String propName = field.getKey();
                            JsonNode prop = field.getValue();
                            String type = prop.get("type").asText();
                            String details = "";
                            if (prop.has("minimum")) {
                                details = " (min: " + prop.get("minimum").asText() + ", max: " + prop.get("maximum").asText() + ")";
                            } else if (prop.has("items")) {
                                details = " (items: " + prop.get("items").get("type").asText() + ")";
                            }
                            props.append(propName).append(": ").append(type).append(details).append("; ");
                        });
                        schemaData.put("Properties", props.toString());
                        schemas.add(schemaData);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        }

        // Load instances
        try (ScanResult scanResult = new ClassGraph().acceptPaths("/instances").enableAllInfo().scan()) {
            scanResult.getResourcesWithExtension("json").forEach(resource -> {
                try (InputStream is = resource.open()) {
                    JsonNode instance = mapper.readTree(is);
                    String uuid = instance.get("uuid").asText();
                    Map<String, String> instanceData = new HashMap<>();
                    String type = resource.getPath().contains("/instances/engines") ? "engines" : "cars";
                    instanceData.put("Type", type);
                    instanceData.put("UUID", uuid);
                    instanceData.put("Name", instance.get("name").asText());
                    StringBuilder attrs = new StringBuilder();
                    instance.fields().forEachRemaining(field -> {
                        if (!field.getKey().equals("uuid") && !field.getKey().equals("name") && !field.getKey().equals("engineRelationships")) {
                            String value = field.getValue().isArray() ? field.getValue().toString() : field.getValue().asText();
                            attrs.append(field.getKey()).append(": ").append(value).append("; ");
                        }
                    });
                    instanceData.put("Attributes", attrs.toString());
                    instanceJson.put(uuid, mapper.writerWithDefaultPrettyPrinter().writeValueAsString(instance));

                    if (type.equals("cars")) {
                        JsonNode rels = instance.get("engineRelationships");
                        instanceData.put("Relationships", rels != null ? rels.toString() : "");
                        List<Map<String, String>> engines = new ArrayList<>();
                        if (rels != null && rels.isArray()) {
                            for (JsonNode rel : rels) {
                                String engineUuid = rel.get("engineUuid").asText();
                                try (InputStream engineIs = MainView.class.getResourceAsStream("/instances/engines/" + engineUuid + ".json")) {
                                    InputStream actualEngineIs = engineIs;
                                    if (engineIs == null) {
                                        String[] parts = engineUuid.split(":");
                                        if (parts.length == 2) {
                                            actualEngineIs = MainView.class.getResourceAsStream("/instances/engines/" + parts[0] + "-" + parts[1] + ".json");
                                        }
                                    }
                                    if (actualEngineIs != null) {
                                        JsonNode engine = mapper.readTree(actualEngineIs);
                                        Map<String, String> engineData = new HashMap<>();
                                        engineData.put("UUID", engine.get("uuid").asText());
                                        engineData.put("Name", engine.get("name").asText());
                                        StringBuilder engineAttrs = new StringBuilder();
                                        engine.fields().forEachRemaining(field -> {
                                            if (!field.getKey().equals("uuid") && !field.getKey().equals("name")) {
                                                String value = field.getValue().isArray() ? field.getValue().toString() : field.getValue().asText();
                                                engineAttrs.append(field.getKey()).append(": ").append(value).append("; ");
                                            }
                                        });
                                        engineData.put("Attributes", engineAttrs.toString());
                                        engineData.put("validFrom", rel.get("validFrom").asText());
                                        engineData.put("validTo", rel.has("validTo") ? rel.get("validTo").asText() : "");
                                        engines.add(engineData);
                                    }
                                }
                            }
                            carEngines.put(uuid, engines);
                        }
                    }
                    instances.add(instanceData);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        }
    }

    private VerticalLayout createSchemasTab() {
        VerticalLayout layout = new VerticalLayout();
        TextField searchField = new TextField("Search Schemas");
        Grid<Map<String, String>> grid = new Grid<>();
        grid.addColumn(m -> m.get("Schema Name")).setHeader("Schema Name");
        grid.addColumn(new ComponentRenderer<>(item -> {
            Span span = new Span(item.get("Properties"));
            span.getElement().setProperty("title", item.get("Properties"));
            return span;
        })).setHeader("Properties").setKey("properties");
        grid.addThemeVariants(GridVariant.LUMO_WRAP_CELL_CONTENT);
        ListDataProvider<Map<String, String>> dataProvider = new ListDataProvider<>(schemas);
        grid.setDataProvider(dataProvider);
        grid.setHeight("1000px");
        grid.setPageSize(100);
        searchField.addValueChangeListener(event -> {
            String filter = event.getValue().toLowerCase();
            dataProvider.setFilter(item -> item.get("Schema Name").toLowerCase().contains(filter));
        });
        layout.add(searchField, grid);
        return layout;
    }

    private VerticalLayout createInstancesTab() {
        VerticalLayout layout = new VerticalLayout();
        TextField searchField = new TextField("Search Instances");
        Grid<Map<String, String>> grid = new Grid<>();
        grid.addColumn(m -> m.get("Type")).setHeader("Type");
        grid.addColumn(new ComponentRenderer<>(item -> {
            Span span = new Span(item.get("UUID"));
            span.getElement().setProperty("title", item.get("UUID"));
            return span;
        })).setHeader("UUID").setKey("uuid");
        grid.addColumn(m -> m.get("Name")).setHeader("Name");
        grid.addColumn(new ComponentRenderer<>(item -> {
            Span span = new Span(item.get("Attributes"));
            span.getElement().setProperty("title", item.get("Attributes"));
            return span;
        })).setHeader("Attributes").setKey("attributes");
        grid.addThemeVariants(GridVariant.LUMO_WRAP_CELL_CONTENT);
        ListDataProvider<Map<String, String>> dataProvider = new ListDataProvider<>(instances);
        grid.setDataProvider(dataProvider);
        grid.setHeight("1000px");
        grid.setPageSize(100);
        searchField.addValueChangeListener(event -> {
            String filter = event.getValue().toLowerCase();
            dataProvider.setFilter(item -> item.get("UUID").toLowerCase().contains(filter) || item.get("Name").toLowerCase().contains(filter));
        });

        // Add selection listener for main grid
        grid.addSelectionListener(event -> {
            selectedItem = event.getFirstSelectedItem().orElse(null);
        });

        // Add context menu for main grid JSON
        ContextMenu contextMenu = new ContextMenu(grid);
        contextMenu.addItem("View JSON", event -> {
            if (selectedItem != null) {
                Dialog dialog = new Dialog();
                dialog.setHeaderTitle("Instance JSON");
                TextArea jsonArea = new TextArea();
                jsonArea.setValue(instanceJson.getOrDefault(selectedItem.get("UUID"), ""));
                jsonArea.setReadOnly(true);
                jsonArea.setWidth("600px");
                jsonArea.setHeight("400px");
                dialog.add(jsonArea);
                Button closeButton = new Button("Close", e -> dialog.close());
                dialog.getFooter().add(closeButton);
                dialog.setModal(true);
                dialog.open();
            }
        });

        // Add expandable details for engines, expanded by default for cars
        grid.setDetailsVisibleOnClick(true);
        grid.setItemDetailsRenderer(new ComponentRenderer<>(item -> {
            VerticalLayout detailsLayout = new VerticalLayout();
            detailsLayout.setPadding(false);
            detailsLayout.setMargin(false);
            if (item.get("Type").equals("cars")) {
                Grid<Map<String, String>> engineGrid = new Grid<>();
                engineGrid.addColumn(new ComponentRenderer<>(engine -> {
                    Span span = new Span(engine.get("UUID"));
                    span.getElement().setProperty("title", engine.get("UUID"));
                    return span;
                })).setHeader("Engine UUID").setKey("engineUuid");
                engineGrid.addColumn(m -> m.get("Name")).setHeader("Name");
                engineGrid.addColumn(new ComponentRenderer<>(engine -> {
                    Span span = new Span(engine.get("Attributes"));
                    span.getElement().setProperty("title", engine.get("Attributes"));
                    return span;
                })).setHeader("Attributes").setKey("engineAttributes");
                engineGrid.addColumn(m -> m.get("validFrom")).setHeader("Valid From");
                engineGrid.addColumn(m -> m.get("validTo")).setHeader("Valid To");
                engineGrid.addThemeVariants(GridVariant.LUMO_WRAP_CELL_CONTENT);
                List<Map<String, String>> engines = carEngines.getOrDefault(item.get("UUID"), new ArrayList<>());
                engineGrid.setItems(engines);
                engineGrid.setHeight(engines.size() * 60 + 80 + "px");

                // Add selection listener for engine sub-grid
                engineGrid.addSelectionListener(event -> {
                    selectedEngine.put(item.get("UUID"), event.getFirstSelectedItem().orElse(null));
                });

                // Add context menu for engine sub-grid JSON
                ContextMenu engineContextMenu = new ContextMenu(engineGrid);
                engineContextMenu.addItem("View Engine JSON", event -> {
                    Map<String, String> selectedEngineItem = selectedEngine.get(item.get("UUID"));
                    if (selectedEngineItem != null) {
                        Dialog dialog = new Dialog();
                        dialog.setHeaderTitle("Engine JSON");
                        TextArea jsonArea = new TextArea();
                        jsonArea.setValue(instanceJson.getOrDefault(selectedEngineItem.get("UUID"), ""));
                        jsonArea.setReadOnly(true);
                        jsonArea.setWidth("600px");
                        jsonArea.setHeight("400px");
                        dialog.add(jsonArea);
                        Button closeButton = new Button("Close", e -> dialog.close());
                        dialog.getFooter().add(closeButton);
                        dialog.setModal(true);
                        dialog.open();
                    }
                });

                detailsLayout.add(engineGrid);
            }
            return detailsLayout;
        }));

        // Expand details by default for car rows
        dataProvider.getItems().forEach(item -> {
            if (item.get("Type").equals("cars")) {
                grid.setDetailsVisible(item, true);
            }
        });

        layout.add(searchField, grid);
        return layout;
    }
}
```

        ### Specific Changes in MainView.java
The changes are in the `MainView` class and `createInstancesTab` method, compared to the previous version (artifact ID `1b48fc4b-77fc-4cb2-b797-ebca601111fb`, version ID `e2fb35f7-9e59-4f58-a8ff-fac8ec0f0d89`).

        #### Previous `MainView` (Class-Level)
        ```java
// Class-level fields
private static final ObjectMapper mapper = new ObjectMapper();
private final List<Map<String, String>> schemas = new ArrayList<>();
private final List<Map<String, String>> instances = new ArrayList<>();
private final Map<String, List<Map<String, String>>> carEngines = new HashMap<>();
private final Map<String, String> instanceJson = new HashMap<>();
private Map<String, String> selectedItem;
```

        #### Updated `MainView` (Class-Level)
        ```java
// Class-level fields
private static final ObjectMapper mapper = new ObjectMapper();
private final List<Map<String, String>> schemas = new ArrayList<>();
private final List<Map<String, String>> instances = new ArrayList<>();
private final Map<String, List<Map<String, String>>> carEngines = new HashMap<>();
private final Map<String, String> instanceJson = new HashMap<>();
private Map<String, String> selectedItem;
private final Map<String, Map<String, String>> selectedEngine = new HashMap<>(); // Added
```

        #### Previous `createInstancesTab` (Relevant Lines)
        ```java
// Lines ~38-57 in ItemDetailsRenderer
grid.setDetailsVisibleOnClick(true);
grid.setItemDetailsRenderer(new ComponentRenderer<>(item -> {
VerticalLayout detailsLayout = new VerticalLayout();
    detailsLayout.setPadding(false);
    detailsLayout.setMargin(false);
    if (item.get("Type").equals("cars")) {
Grid<Map<String, String>> engineGrid = new Grid<>();
        engineGrid.addColumn(new ComponentRenderer<>(engine -> {
Span span = new Span(engine.get("UUID"));
            span.getElement().setProperty("title", engine.get("UUID"));
        return span;
        })).setHeader("Engine UUID").setKey("engineUuid");
        engineGrid.addColumn(m -> m.get("Name")).setHeader("Name");
        engineGrid.addColumn(new ComponentRenderer<>(engine -> {
Span span = new Span(engine.get("Attributes"));
            span.getElement().setProperty("title", engine.get("Attributes"));
        return span;
        })).setHeader("Attributes").setKey("engineAttributes");
        engineGrid.addColumn(m -> m.get("validFrom")).setHeader("Valid From");
        engineGrid.addColumn(m -> m.get("validTo")).setHeader("Valid To");
        engineGrid.addThemeVariants(GridVariant.LUMO_WRAP_CELL_CONTENT);
List<Map<String, String>> engines = carEngines.getOrDefault(item.get("UUID"), new ArrayList<>());
        engineGrid.setItems(engines);
        engineGrid.setHeight(engines.size() * 60 + 80 + "px");
        detailsLayout.add(engineGrid);
    }
            return detailsLayout;
}));
        ```

        #### Updated `createInstancesTab` (Relevant Lines)
        ```java
// Lines ~38-66 in ItemDetailsRenderer
grid.setDetailsVisibleOnClick(true);
grid.setItemDetailsRenderer(new ComponentRenderer<>(item -> {
VerticalLayout detailsLayout = new VerticalLayout();
    detailsLayout.setPadding(false);
    detailsLayout.setMargin(false);
    if (item.get("Type").equals("cars")) {
Grid<Map<String, String>> engineGrid = new Grid<>();
        engineGrid.addColumn(new ComponentRenderer<>(engine -> {
Span span = new Span(engine.get("UUID"));
            span.getElement().setProperty("title", engine.get("UUID"));
        return span;
        })).setHeader("Engine UUID").setKey("engineUuid");
        engineGrid.addColumn(m -> m.get("Name")).setHeader("Name");
        engineGrid.addColumn(new ComponentRenderer<>(engine -> {
Span span = new Span(engine.get("Attributes"));
            span.getElement().setProperty("title", engine.get("Attributes"));
        return span;
        })).setHeader("Attributes").setKey("engineAttributes");
        engineGrid.addColumn(m -> m.get("validFrom")).setHeader("Valid From");
        engineGrid.addColumn(m -> m.get("validTo")).setHeader("Valid To");
        engineGrid.addThemeVariants(GridVariant.LUMO_WRAP_CELL_CONTENT);
List<Map<String, String>> engines = carEngines.getOrDefault(item.get("UUID"), new ArrayList<>());
        engineGrid.setItems(engines);
        engineGrid.setHeight(engines.size() * 60 + 80 + "px");

        // Add selection listener for engine sub-grid
        engineGrid.addSelectionListener(event -> {
        selectedEngine.put(item.get("UUID"), event.getFirstSelectedItem().orElse(null));
        });

// Add context menu for engine sub-grid JSON
ContextMenu engineContextMenu = new ContextMenu(engineGrid);
        engineContextMenu.addItem("View Engine JSON", event -> {
Map<String, String> selectedEngineItem = selectedEngine.get(item.get("UUID"));
            if (selectedEngineItem != null) {
Dialog dialog = new Dialog();
                dialog.setHeaderTitle("Engine JSON");
TextArea jsonArea = new TextArea();
                jsonArea.setValue(instanceJson.getOrDefault(selectedEngineItem.get("UUID"), ""));
        jsonArea.setReadOnly(true);
                jsonArea.setWidth("600px");
                jsonArea.setHeight("400px");
                dialog.add(jsonArea);
Button closeButton = new Button("Close", e -> dialog.close());
                dialog.getFooter().add(closeButton);
                dialog.setModal(true);
                dialog.open();
        }
        });

        detailsLayout.add(engineGrid);
        }
        return detailsLayout;
        }));
        ```

        ### Specific Line-by-Line Changes
        #### Class-Level
        1. **Added Line ~26 (Class-Level Field)**:
        - **Code**:
        ```java
        private final Map<String, Map<String, String>> selectedEngine = new HashMap<>();
        ```
        - **Purpose**: Added a `Map` to store the selected engine for each car’s sub-grid, keyed by the car’s `UUID` to handle multiple open sub-grids.
        - **Impact**: Tracks the selected engine row in each sub-grid for the `ContextMenu`.

        #### createInstancesTab
        1. **Added Lines ~47-65 (Sub-Grid SelectionListener and ContextMenu)**:
        - **Code**:
        ```java
        engineGrid.addSelectionListener(event -> {
        selectedEngine.put(item.get("UUID"), event.getFirstSelectedItem().orElse(null));
        });
        ContextMenu engineContextMenu = new ContextMenu(engineGrid);
        engineContextMenu.addItem("View Engine JSON", event -> {
        Map<String, String> selectedEngineItem = selectedEngine.get(item.get("UUID"));
        if (selectedEngineItem != null) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Engine JSON");
        TextArea jsonArea = new TextArea();
        jsonArea.setValue(instanceJson.getOrDefault(selectedEngineItem.get("UUID"), ""));
        jsonArea.setReadOnly(true);
        jsonArea.setWidth("600px");
        jsonArea.setHeight("400px");
        dialog.add(jsonArea);
        Button closeButton = new Button("Close", e -> dialog.close());
        dialog.getFooter().add(closeButton);
        dialog.setModal(true);
        dialog.open();
        }
        });
        ```
        - **Purpose**: Added a `SelectionListener` to `engineGrid` to store the selected engine row in `selectedEngine`, keyed by the car’s `UUID`. Added a `ContextMenu` to `engineGrid` with a “View Engine JSON” item that opens a `Dialog` with the selected engine’s JSON (e.g., `engine-gas-prod.json`).
        - **Impact**: Right-clicking an engine row in the sub-grid shows the engine’s JSON, while right-clicking the main Grid shows the car/engine instance’s JSON.

        ### Unchanged Aspects
        - **ContextMenu for Main Grid**: Retained as is, opens only on right-click (no `setOpenOnClick(true)`), uses `selectedItem` for JSON display.
        - **Sub-Grid Height**: Unchanged, `engineGrid.setHeight(engines.size() * 60 + 80 + "px")` ensures all engines (e.g., 3 for `car:sedan-luxury`) are visible without scrolling.
        - **Truncation**: Unchanged, `LUMO_WRAP_CELL_CONTENT` and tooltips (`Span` with `title`) for `UUID`, `Attributes`, and sub-grid columns.
        - **Files**: No changes to `car-engine-json-core` files (`EngineService.java`, `GenerateVisuals.java`, `Main.java`, `AppTest.java`, JSON schemas/instances, `visualize.html`, `relationships.json`, `schema-type-mapping.json`), POMs, or UI module’s `Main.java`, `application.properties`.

        ### Behavior in UI
        - **Schemas Tab**: Grid with `Schema Name`, `Properties` (wrapped text, tooltip), searchable, 1000px height, 100 items per page.
        - **Instances Tab**:
        - Grid columns: `Type`, `UUID`, `Name`, `Attributes` (wrapped text, tooltips).
        - **Right-Click on Main Grid**: Opens `ContextMenu` with “View JSON,” showing the JSON of the selected car/engine instance (e.g., `car-sedan.json`).
        - **Right-Click on Engine Sub-Grid**: Opens `ContextMenu` with “View Engine JSON,” showing the JSON of the selected engine (e.g., `engine-gas-prod.json`).
        - **Left-Click**: Toggles expandable details, no `ContextMenu`.
        - Car rows have details expanded by default, showing engine sub-grid (`Engine UUID`, `Name`, `Attributes`, `validFrom`, `validTo`, with wrapped text and tooltips). Sub-grid height fits all engines without scrolling.
        - Grid height: 1000px, page size: 100, scrollable.
        - **Access**: `http://localhost:8080` after `mvn spring-boot:run`.

        ### Workflow (Unchanged)
        1. **Setup Mono-Repo**:
        ```bash
        mvn archetype:generate -DgroupId=com.example -DartifactId=car-engine-json -Dversion=1.0-SNAPSHOT -DarchetypeArtifactId=maven-archetype-quickstart -DinteractiveMode=false
        cd car-engine-json
        mvn archetype:generate -DgroupId=com.example -DartifactId=car-engine-json-core -Dversion=1.0-SNAPSHOT -DarchetypeArtifactId=maven-archetype-quickstart -DinteractiveMode=false
        mvn archetype:generate -DgroupId=com.example -DartifactId=car-engine-json-ui -Dversion=1.0-SNAPSHOT -DarchetypeArtifactId=maven-archetype-quickstart -DinteractiveMode=false
        ```
        2. **Replace POMs and Copy Files**: As described previously.
        3. **Build**:
        ```bash
        mvn clean install
        ```
        4. **Run Tests**:
        ```bash
        cd car-engine-json-core
        mvn test
        ```
        5. **Generate Visualizations**:
        ```bash
        java -cp target/classes com.example.engine.GenerateVisuals
        ```
        6. **Run UI**:
        ```bash
        cd car-engine-json-ui
        mvn spring-boot:run
        ```

        ### Notes
        - **JSON Dialog**: Right-click on engine sub-grid rows now shows engine JSON; main Grid right-click shows car/engine instance JSON.
        - **Sub-Grid Height**: Retained `60px` per row + `80px` header to ensure no scrolling.
        - **Truncation**: Unchanged, `LUMO_WRAP_CELL_CONTENT` and tooltips for `UUID`, `Attributes`.
        - **Mono-Repo**: UI depends on core JAR, uses `ClassGraph`.
        - **Apache 2.0**: Vaadin components (`ContextMenu`, `Dialog`, `Grid`) used, no commercial dependencies.

        If you encounter issues (e.g., incorrect JSON in `Dialog`, sub-grid scrolling, or IDE errors), please share details (e.g., browser console output, screenshots, IDE version), and I’ll assist promptly!