package com.example.engine;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.data.provider.ListDataProvider;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.Route;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

@Route("")
public class MainView extends VerticalLayout {
    private static final ObjectMapper mapper = new ObjectMapper();
    private final List<Map<String, String>> schemas = new ArrayList<>();
    private final List<Map<String, String>> instances = new ArrayList<>();
    private final Map<String, List<Map<String, String>>> carEngines = new HashMap<>();
    private final Map<String, String> instanceJson = new HashMap<>();

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
        // Load schemas
        try (Stream<Path> paths = Files.walk(Paths.get("src/main/resources/schemas"))) {
            paths.filter(Files::isRegularFile)
                    .forEach(path -> {
                        try {
                            JsonNode schema = mapper.readTree(path.toFile());
                            JsonNode properties = schema.get("properties");
                            if (properties != null) {
                                Map<String, String> schemaData = new HashMap<>();
                                schemaData.put("Schema Name", path.getFileName().toString());
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
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Load instances and relationships
        try (Stream<Path> paths = Files.walk(Paths.get("src/main/resources/instances"))) {
            paths.filter(Files::isRegularFile)
                    .forEach(path -> {
                        try {
                            JsonNode instance = mapper.readTree(path.toFile());
                            String uuid = instance.get("uuid").asText();
                            Map<String, String> instanceData = new HashMap<>();
                            String type = path.getParent().getFileName().toString();
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
                                        Path enginePath = Paths.get("src/main/resources/instances/engines", engineUuid + ".json");
                                        if (!Files.exists(enginePath)) {
                                            String[] parts = engineUuid.split(":");
                                            if (parts.length == 2) {
                                                enginePath = Paths.get("src/main/resources/instances/engines", parts[0] + "-" + parts[1] + ".json");
                                            }
                                        }
                                        if (Files.exists(enginePath)) {
                                            JsonNode engine = mapper.readTree(enginePath.toFile());
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
                            instances.add(instanceData);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private VerticalLayout createSchemasTab() {
        VerticalLayout layout = new VerticalLayout();
        TextField searchField = new TextField("Search Schemas");
        Grid<Map<String, String>> grid = new Grid<>();
        grid.addColumn(m -> m.get("Schema Name")).setHeader("Schema Name");
        grid.addColumn(m -> m.get("Properties")).setHeader("Properties");
        ListDataProvider<Map<String, String>> dataProvider = new ListDataProvider<>(schemas);
        grid.setDataProvider(dataProvider);
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
        grid.addColumn(m -> m.get("UUID")).setHeader("UUID");
        grid.addColumn(m -> m.get("Name")).setHeader("Name");
        grid.addColumn(m -> m.get("Attributes")).setHeader("Attributes");
        ListDataProvider<Map<String, String>> dataProvider = new ListDataProvider<>(instances);
        grid.setDataProvider(dataProvider);
        searchField.addValueChangeListener(event -> {
            String filter = event.getValue().toLowerCase();
            dataProvider.setFilter(item -> item.get("UUID").toLowerCase().contains(filter) || item.get("Name").toLowerCase().contains(filter));
        });

        // Add expandable details for engines and full JSON
        grid.setDetailsVisibleOnClick(true);
        grid.setItemDetailsRenderer(new ComponentRenderer<>(item -> {
            VerticalLayout detailsLayout = new VerticalLayout();
            detailsLayout.setPadding(false);
            detailsLayout.setMargin(false);

            // Add full JSON
            TextArea jsonArea = new TextArea("Full JSON");
            jsonArea.setValue(instanceJson.getOrDefault(item.get("UUID"), ""));
            jsonArea.setReadOnly(true);
            jsonArea.setWidthFull();
            detailsLayout.add(jsonArea);

            // Add engine sub-grid for cars
            if (item.get("Type").equals("cars")) {
                Grid<Map<String, String>> engineGrid = new Grid<>();
                engineGrid.addColumn(m -> m.get("UUID")).setHeader("Engine UUID");
                engineGrid.addColumn(m -> m.get("Name")).setHeader("Name");
                engineGrid.addColumn(m -> m.get("Attributes")).setHeader("Attributes");
                engineGrid.addColumn(m -> m.get("validFrom")).setHeader("Valid From");
                engineGrid.addColumn(m -> m.get("validTo")).setHeader("Valid To");
                List<Map<String, String>> engines = carEngines.getOrDefault(item.get("UUID"), new ArrayList<>());
                engineGrid.setItems(engines);
                detailsLayout.add(engineGrid);
            }

            return detailsLayout;
        }));

        layout.add(searchField, grid);
        return layout;
    }
}