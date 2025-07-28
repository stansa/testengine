package com.example;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vaadin.flow.component.accordion.Accordion;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.contextmenu.ContextMenu;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.data.provider.ListDataProvider;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.Route;

import io.github.classgraph.ClassGraph;
import io.github.classgraph.ScanResult;

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

    public MainView() {
        loadData();

        // Create tabs
        Tab versionTab = new Tab("Version");
        Tab viewModifyTab = new Tab("View/Modify");
        Tabs tabs = new Tabs(versionTab, viewModifyTab);
        VerticalLayout versionLayout = createVersionTab();
        VerticalLayout viewModifyLayout = createViewModifyTab();
        Map<Tab, VerticalLayout> tabsToLayouts = new HashMap<>();
        tabsToLayouts.put(versionTab, versionLayout);
        tabsToLayouts.put(viewModifyTab, viewModifyLayout);

        // Show selected tab content
        tabs.addSelectedChangeListener(event -> {
            removeAll();
            add(tabs, tabsToLayouts.get(event.getSelectedTab()));
        });

        add(tabs, versionLayout);
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
                        }
                        carEngines.put(uuid, engines);
                    }
                    instances.add(instanceData);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        }
    }

    private VerticalLayout createVersionTab() {
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

        // Add selection listener for workaround
        grid.addSelectionListener(event -> {
            selectedItem = event.getFirstSelectedItem().orElse(null);
        });

        // Add context menu for JSON
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

    private VerticalLayout createViewModifyTab() {
        VerticalLayout layout = new VerticalLayout();
        Div jsonFormsDiv = new Div();
        jsonFormsDiv.setWidthFull();
        jsonFormsDiv.setHeight("1000px");

        // Load example schema and data (replace with actual loading from files)
        String schema = "{\"type\": \"object\", \"properties\": {\"name\": {\"type\": \"string\"}, \"uuid\": {\"type\": \"string\"}}}";
        String uiSchema = "{}"; // Optional UI schema
        String data = "{}"; // Initial data

        jsonFormsDiv.getElement().executeJs("jsonforms.renderForm($0, $1, $2, $3);", jsonFormsDiv.getId().get(), schema, uiSchema, data);
        layout.add(jsonFormsDiv);
        return layout;
    }
}
