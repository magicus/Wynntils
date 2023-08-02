/*
 * Copyright © Wynntils 2022-2023.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.core.persisted.config;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.wynntils.core.WynntilsMod;
import com.wynntils.core.components.Manager;
import com.wynntils.core.components.Managers;
import com.wynntils.core.consumers.features.Configurable;
import com.wynntils.core.consumers.features.Feature;
import com.wynntils.core.consumers.features.FeatureManager;
import com.wynntils.core.consumers.features.Translatable;
import com.wynntils.core.consumers.overlays.DynamicOverlay;
import com.wynntils.core.consumers.overlays.Overlay;
import com.wynntils.core.consumers.overlays.OverlayManager;
import com.wynntils.core.json.JsonManager;
import com.wynntils.core.persisted.Persisted;
import com.wynntils.core.persisted.upfixers.ConfigUpfixerManager;
import com.wynntils.utils.JsonUtils;
import com.wynntils.utils.mc.McUtils;
import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;
import org.apache.commons.lang3.reflect.FieldUtils;

public final class ConfigManager extends Manager {
    private static final File CONFIG_DIR = WynntilsMod.getModStorageDir("config");
    private static final String FILE_SUFFIX = ".conf.json";
    private static final File DEFAULT_CONFIG = new File(CONFIG_DIR, "default" + FILE_SUFFIX);
    private static final String OVERLAY_GROUPS_JSON_KEY = "overlayGroups";
    private static final Set<Config<?>> CONFIGS = new TreeSet<>();

    private final File userConfig;
    private JsonObject configObject;

    public ConfigManager(
            ConfigUpfixerManager configUpfixerManager,
            JsonManager jsonManager,
            FeatureManager feature,
            OverlayManager overlay) {
        super(List.of(configUpfixerManager, jsonManager, feature, overlay));

        userConfig = new File(CONFIG_DIR, McUtils.mc().getUser().getUuid() + FILE_SUFFIX);
    }

    public void init() {
        // First, we load the config file
        configObject = Managers.Json.loadPreciousJson(userConfig);

        // Register all features and overlays
        Managers.Feature.getFeatures().forEach(this::registerFeature);

        // Now, we have to apply upfixers, before any config loading happens
        if (Managers.ConfigUpfixer.runUpfixers(configObject, CONFIGS)) {
            Managers.Json.savePreciousJson(userConfig, configObject);
        }

        // Finish off the config init process

        // Load configs for all features
        Managers.Config.reloadConfiguration();

        // Save config file after loading all configurables' options
        Managers.Config.saveConfig();

        // Create default config file containing all configurables' options
        Managers.Config.saveDefaultConfig();
    }

    private void registerFeature(Feature feature) {
        registerConfigOptions(feature);

        for (Overlay overlay : Managers.Overlay.getFeatureOverlays(feature).stream()
                .filter(overlay -> Managers.Overlay.getFeatureOverlayGroups(feature).stream()
                        .noneMatch(overlayGroupHolder ->
                                overlayGroupHolder.getOverlays().contains(overlay)))
                .toList()) {
            registerConfigOptions(overlay);
        }
    }

    private <P extends Configurable & Translatable> void registerConfigOptions(P configurable) {
        List<Config<?>> configs = getConfigOptions(configurable);

        configurable.addConfigOptions(configs);
        CONFIGS.addAll(configs);
    }

    public void reloadConfiguration() {
        configObject = Managers.Json.loadPreciousJson(userConfig);
        loadConfigOptions(true, true);
    }

    // Info: The purpose of initOverlayGroups is to use the config system in a way that is really "hacky".
    //       Overlay group initialization needs:
    //          1, Overlay instances to be loaded (at init, the default number of instances, then the number defined in
    //             configObject)
    //          2, We need to handle dynamic overlays' configs as regular configs, so that they can be loaded from the
    //             config file
    //       The problem is that the config system "save" is used to remove unused configs, and that "load" is used to
    //       init dynamic overlay instances.
    //
    //       This really becomes a problem when modifying overlay group sizes at runtime.
    //       We want to do 4 things: Save new overlay group size, init overlay instances, load configs, and remove
    //       unused configs.
    //       This means we need to save - load - save, which we should not do. initOverlayGroups is the solution to
    //       this, for now.
    public void loadConfigOptions(boolean resetIfNotFound, boolean initOverlayGroups) {
        // We have to set up the overlay groups first, so that the overlays' configs can be loaded
        JsonObject overlayGroups = JsonUtils.getNullableJsonObject(configObject, OVERLAY_GROUPS_JSON_KEY);

        for (OverlayGroupHolder holder : Managers.Overlay.getOverlayGroups()) {
            if (initOverlayGroups) {
                if (overlayGroups.has(holder.getConfigKey())) {
                    JsonArray ids = JsonUtils.getNullableJsonArray(overlayGroups, holder.getConfigKey());

                    List<Integer> idList =
                            ids.asList().stream().map(JsonElement::getAsInt).toList();

                    Managers.Overlay.createOverlayGroupWithIds(holder, idList);
                } else {
                    Managers.Overlay.createOverlayGroupWithDefaults(holder);
                }
            }

            holder.getOverlays().forEach(overlay -> overlay.addConfigOptions(this.getConfigOptions(overlay)));
        }

        for (Config<?> config : getConfigList()) {
            // option hasn't been saved to config
            if (!configObject.has(config.getJsonName())) {
                if (resetIfNotFound) {
                    config.reset();
                }
                continue;
            }

            // read value and update option
            JsonElement configJson = configObject.get(config.getJsonName());
            Object value = Managers.Json.GSON.fromJson(configJson, config.getType());
            config.restoreValue(value);
        }

        // Newly created group overlays need to be enabled
        for (OverlayGroupHolder holder : Managers.Overlay.getOverlayGroups()) {
            Managers.Overlay.enableOverlays(holder.getParent());
        }
    }

    private static List<Config<?>> getConfigList() {
        // This breaks the concept of "manager holds all config holders at all times". Instead we get the group
        // overlays' configs from the overlay instance itself, to save us some trouble.

        return Stream.concat(
                        CONFIGS.stream(),
                        Managers.Overlay.getOverlayGroups().stream()
                                .map(OverlayGroupHolder::getOverlays)
                                .flatMap(List::stream)
                                .map(Overlay::getConfigOptions)
                                .flatMap(List::stream))
                .toList();
    }

    public void saveConfig() {
        // create json object, with entry for each option of each container
        JsonObject configJson = new JsonObject();
        for (Config<?> config : getConfigList()) {
            if (!config.valueChanged()) continue; // only save options that have been set by the user
            Object value = config.getValue();

            JsonElement configElement = Managers.Json.GSON.toJsonTree(value);
            configJson.add(config.getJsonName(), configElement);
        }

        // Also save upfixer data
        configJson.add(
                Managers.ConfigUpfixer.UPFIXER_JSON_MEMBER_NAME,
                configObject.get(Managers.ConfigUpfixer.UPFIXER_JSON_MEMBER_NAME));

        // Save overlay groups
        JsonObject overlayGroups = new JsonObject();
        for (OverlayGroupHolder holder : Managers.Overlay.getOverlayGroups()) {
            JsonArray ids = new JsonArray();

            holder.getOverlays().stream()
                    .map(overlay -> ((DynamicOverlay) overlay).getId())
                    .forEach(ids::add);

            overlayGroups.add(holder.getConfigKey(), ids);
        }

        configJson.add(OVERLAY_GROUPS_JSON_KEY, overlayGroups);

        Managers.Json.savePreciousJson(userConfig, configJson);
    }

    private void saveDefaultConfig() {
        // create json object, with entry for each option of each container
        JsonObject configJson = new JsonObject();
        for (Config<?> config : getConfigList()) {
            Object value = config.getDefaultValue();

            JsonElement configElement = Managers.Json.GSON.toJsonTree(value);
            configJson.add(config.getJsonName(), configElement);
        }

        WynntilsMod.info("Creating default config file with " + configJson.size() + " config values.");
        Managers.Json.savePreciousJson(DEFAULT_CONFIG, configJson);
    }

    private <P extends Configurable & Translatable> List<Config<?>> getConfigOptions(P owner) {
        Managers.Persisted.verifyAnnotations(owner);

        List<Config<?>> options = new ArrayList<>();
        options.addAll(Managers.Persisted.getPersisted(owner, Config.class).stream()
                .map(p -> processConfig(owner, p.a(), p.b()))
                .toList());
        return options;
    }

    private static <P extends Configurable & Translatable> Config<?> processConfig(
            P owner, Field configField, Persisted configInfo) {
        Config<?> configObj;
        try {
            configObj = (Config<?>) FieldUtils.readField(configField, owner, true);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Cannot read Config field: " + configField, e);
        }

        configObj.createConfigHolder(owner, configField, configInfo);

        if (WynntilsMod.isDevelopmentEnvironment()) {
            if (configObj.isVisible()) {
                if (configObj.getDisplayName().startsWith("feature.wynntils.")) {
                    WynntilsMod.error("Config displayName i18n is missing for " + configObj.getDisplayName());
                    throw new AssertionError("Missing i18n for " + configObj.getDisplayName());
                }
                if (configObj.getDescription().startsWith("feature.wynntils.")) {
                    WynntilsMod.error("Config description i18n is missing for " + configObj.getDescription());
                    throw new AssertionError("Missing i18n for " + configObj.getDescription());
                }
                if (configObj.getDescription().isEmpty()) {
                    WynntilsMod.error("Config description is empty for " + configObj.getDisplayName());
                    throw new AssertionError("Missing i18n for " + configObj.getDisplayName());
                }
            }
        }
        return configObj;
    }

    public Stream<Config<?>> getConfigs() {
        return getConfigList().stream();
    }
}
