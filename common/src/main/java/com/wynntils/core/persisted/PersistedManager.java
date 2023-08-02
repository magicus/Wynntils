/*
 * Copyright © Wynntils 2023.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.core.persisted;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.wynntils.core.WynntilsMod;
import com.wynntils.core.components.Manager;
import com.wynntils.core.components.Managers;
import com.wynntils.core.consumers.features.FeatureManager;
import com.wynntils.core.mod.event.WynncraftConnectionEvent;
import com.wynntils.core.persisted.json.JsonManager;
import com.wynntils.utils.mc.McUtils;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.apache.commons.lang3.reflect.FieldUtils;

public final class PersistedManager extends Manager {
    private static final long SAVE_INTERVAL = 10_000;

    private static final File STORAGE_DIR = WynntilsMod.getModStorageDir("persisted");
    private static final String FILE_SUFFIX = ".data.json";
    private final File userPersistedFile;

    private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
    private final Map<PersistedValue<?>, PersistedMetadata<?>> persisteds = new TreeMap<>();

    private long lastPersisted;
    private boolean scheduledPersist;

    private boolean persistedInitialized = false;

    public PersistedManager(JsonManager jsonManager, FeatureManager feature) {
        super(List.of(jsonManager, feature));
        userPersistedFile = new File(STORAGE_DIR, McUtils.mc().getUser().getUuid() + FILE_SUFFIX);
    }

    public void initComponents() {
        readFromJson();
    }

    public void initFeatures() {
        // Register all persistedables
        //    Managers.Feature.getFeatures().forEach(this::registerPersistedable);

        readFromJson();

        persistedInitialized = true;

        // We might have missed a persist call in between feature init and persisted manager init
        persist();
    }

    public void registerPersistedable(PersistedOwner persistedOwner) {
        Field[] annotatedPersisteds =
                FieldUtils.getFieldsWithAnnotation(persistedOwner.getClass(), RegisterPersisted.class);
        for (Field field : annotatedPersisteds) {
            try {
                Object fieldValue = FieldUtils.readField(field, persistedOwner, true);
                if (!(fieldValue instanceof PersistedValue<?>)) {
                    throw new RuntimeException(
                            "A non-PersistedValue class was marked with @RegisterPersisted annotation: " + field);
                }
            } catch (IllegalAccessException e) {
                throw new RuntimeException("Failed to read @RegisterPersisted annotated field: " + field, e);
            }
        }
        List<Field> fields = FieldUtils.getAllFieldsList(persistedOwner.getClass());
        List<Field> persistedFields = fields.stream()
                .filter(f -> f.getType().equals(PersistedValue.class))
                .toList();

        for (Field persistedField : persistedFields) {
            try {
                RegisterPersisted persistedInfo = Arrays.stream(annotatedPersisteds)
                        .filter(f -> f.equals(persistedField))
                        .findFirst()
                        .map(f -> f.getAnnotation(RegisterPersisted.class))
                        .orElse(null);
                if (persistedInfo == null) {
                    throw new RuntimeException(
                            "A PersistedValue is missing @RegisterPersisted annotation:" + persistedField);
                }

                PersistedValue<?> persisted =
                        (PersistedValue<?>) FieldUtils.readField(persistedField, persistedOwner, true);
                Type valueType = Managers.Json.getJsonValueType(persistedField);

                // FIXME
                Object defaultValue = null;
                String i18nKey = null;
                boolean allowNull = false;

                PersistedMetadata metadata = new PersistedMetadata<>(
                        persistedOwner, persistedField.getName(), valueType, defaultValue, i18nKey, allowNull);
                persisteds.put(persisted, metadata);

            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @SubscribeEvent
    public void onWynncraftDisconnect(WynncraftConnectionEvent.Disconnected event) {
        // Always save when disconnecting
        writeToJson();
    }

    void persist() {
        // We cannot persist before the persisted is initialized, or we will overwrite our persisted
        if (!persistedInitialized || scheduledPersist) return;

        long now = System.currentTimeMillis();
        long delay = Math.max((lastPersisted + SAVE_INTERVAL) - now, 0);

        executor.schedule(
                () -> {
                    scheduledPersist = false;
                    lastPersisted = System.currentTimeMillis();
                    writeToJson();
                },
                delay,
                TimeUnit.MILLISECONDS);
        scheduledPersist = true;
    }

    private void readFromJson() {
        JsonObject persistedJson = Managers.Json.loadPreciousJson(userPersistedFile);
        persisteds.forEach((persisted, metadata) -> {
            String jsonName = metadata.getJsonName();
            if (!persistedJson.has(jsonName)) return;

            // read value and update option
            JsonElement jsonElem = persistedJson.get(jsonName);
            Type valueType = persisteds.get(persisted).getValueType();
            Object value = Managers.Json.GSON.fromJson(jsonElem, valueType);
            persisted.restoreValue(value);

            PersistedOwner owner = persisteds.get(persisted).getOwner();
            owner.onPersistedLoad();
        });
    }

    private void writeToJson() {
        JsonObject persistedJson = new JsonObject();

        persisteds.forEach((persisted, metadata) -> {
            String jsonName = metadata.getJsonName();
            Type valueType = persisteds.get(persisted).getValueType();
            JsonElement jsonElem = Managers.Json.GSON.toJsonTree(persisted.get(), valueType);
            persistedJson.add(jsonName, jsonElem);
        });

        Managers.Json.savePreciousJson(userPersistedFile, persistedJson);
    }
}
