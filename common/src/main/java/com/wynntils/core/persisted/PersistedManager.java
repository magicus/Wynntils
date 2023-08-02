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
import com.wynntils.core.json.JsonManager;
import com.wynntils.core.mod.event.WynncraftConnectionEvent;
import com.wynntils.core.persisted.config.Config;
import com.wynntils.core.persisted.config.NullableConfig;
import com.wynntils.utils.mc.McUtils;
import com.wynntils.utils.type.Pair;
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

    public void registerOwner(PersistedOwner owner) {
        Managers.Persisted.verifyAnnotations(owner);

        Managers.Persisted.getPersisted(owner, Config.class).stream().forEach(p -> {
            Field configField = p.a();
            Config<?> configObj;
            try {
                configObj = (Config<?>) FieldUtils.readField(configField, owner, true);
            } catch (IllegalAccessException e) {
                throw new RuntimeException("Cannot read Config field: " + configField, e);
            }

            PersistedMetadata<?> metadata =
                    Managers.Persisted.createMetadata((PersistedValue<?>) configObj, owner, configField, p.b());
            persisteds.put(configObj, metadata);
        });
    }

    public List<Pair<Field, Persisted>> getPersisted(PersistedOwner owner, Class<? extends PersistedValue> clazzType) {
        // Get pairs of field and annotation for all persisted values of the requested type
        return Arrays.stream(FieldUtils.getFieldsWithAnnotation(owner.getClass(), Persisted.class))
                .filter(field -> clazzType.isAssignableFrom(field.getType()))
                .map(field -> Pair.of(field, field.getAnnotation(Persisted.class)))
                .toList();
    }

    public void verifyAnnotations(PersistedOwner owner) {
        // Verify that only persistable fields are annotated
        Arrays.stream(FieldUtils.getFieldsWithAnnotation(owner.getClass(), Persisted.class))
                .forEach(field -> {
                    if (!PersistedValue.class.isAssignableFrom(field.getType())) {
                        throw new RuntimeException(
                                "A non-persistable class was marked with @Persisted annotation: " + field);
                    }
                });

        // Verify that we have not missed to annotate a persistable field
        FieldUtils.getAllFieldsList(owner.getClass()).stream()
                .filter(field -> PersistedValue.class.isAssignableFrom(field.getType()))
                .forEach(field -> {
                    Persisted annotation = field.getAnnotation(Persisted.class);
                    if (annotation == null) {
                        throw new RuntimeException("A persisted datatype is missing @Persisted annotation:" + field);
                    }
                });
    }

    public void setRaw(PersistedValue<?> persisted, Object value) {
        persisted.setRaw(value);
    }

    private <T> PersistedMetadata<T> createMetadata(
            PersistedValue<T> persisted, PersistedOwner owner, Field configField, Persisted annotation) {
        Type valueType = Managers.Json.getJsonValueType(configField);
        String fieldName = configField.getName();

        String i18nKey = annotation.i18nKey();

        T defaultValue = persisted.get();
        boolean allowNull = persisted.get() instanceof NullableConfig;

        return new PersistedMetadata<T>(owner, fieldName, valueType, defaultValue, i18nKey, allowNull);
    }

    public <T> PersistedMetadata<T> getMetadata(PersistedValue<T> persisted) {
        return (PersistedMetadata<T>) persisteds.get(persisted);
    }

    /// ============================================================================================

    public void initFeatures() {
        readFromJson();

        persistedInitialized = true;

        // We might have missed a persist call in between feature init and persisted manager init
        persist();
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
            setRaw(persisted, value);

            PersistedOwner owner = persisteds.get(persisted).getOwner();
            // FIXME
            // owner.onPersistedLoad();
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
