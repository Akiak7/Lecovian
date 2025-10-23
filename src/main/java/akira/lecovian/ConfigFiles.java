package akira.lecovian;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.fml.common.registry.EntityEntry;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

public final class ConfigFiles {
    public static final class GeneralCfg {
        public long globalSeed = 1337L;
        public double sigmaDivisor = 3.0; // larger => more center-heavy (subtler)
    }

    public static GeneralCfg GENERAL = new GeneralCfg();
    public static Map<String, Boolean> ENTITY_ENABLED = new LinkedHashMap<>(); // id -> enabled

    private static File generalFile;
    private static File entitiesFile;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static void loadOrCreate(File configDir) {
        File myDir = new File(configDir, Lecovian.MODID);
        if (!myDir.exists()) myDir.mkdirs();
        generalFile = new File(myDir, "general.json");
        entitiesFile = new File(myDir, "entities.json");

        // General
        if (generalFile.exists()) {
            try (Reader r = new InputStreamReader(new FileInputStream(generalFile), StandardCharsets.UTF_8)) {
                GENERAL = GSON.fromJson(r, GeneralCfg.class);
                if (GENERAL == null) GENERAL = new GeneralCfg();
            } catch (Exception ignored) {}
        } else {
            saveGeneral();
        }

        // Entities: populate with all living entity IDs (default enabled = true)
        if (entitiesFile.exists()) {
            try (Reader r = new InputStreamReader(new FileInputStream(entitiesFile), StandardCharsets.UTF_8)) {
                JsonObject obj = GSON.fromJson(r, JsonObject.class);
                if (obj != null) {
                    for (Map.Entry<String, ?> e : obj.entrySet()) {
                        ENTITY_ENABLED.put(e.getKey(), obj.get(e.getKey()).getAsBoolean());
                    }
                }
            } catch (Exception ignored) {}
        }

        // Ensure all current living entities are present (new mods, etc.)
        for (EntityEntry ee : ForgeRegistries.ENTITIES) {
            Class<? extends Entity> c = ee.getEntityClass();
            if (!EntityLivingBase.class.isAssignableFrom(c)) continue;
            String id = ee.getRegistryName() == null ? c.getName() : ee.getRegistryName().toString();
            ENTITY_ENABLED.putIfAbsent(id, true); // default: enabled (affect unless user blacklists)
        }
        saveEntities();
    }

    public static void saveGeneral() {
        try (Writer w = new OutputStreamWriter(new FileOutputStream(generalFile), StandardCharsets.UTF_8)) {
            GSON.toJson(GENERAL, w);
        } catch (Exception ignored) {}
    }

    public static void saveEntities() {
        JsonObject o = new JsonObject();
        for (Map.Entry<String, Boolean> e : ENTITY_ENABLED.entrySet()) o.addProperty(e.getKey(), e.getValue());
        try (Writer w = new OutputStreamWriter(new FileOutputStream(entitiesFile), StandardCharsets.UTF_8)) {
            GSON.toJson(o, w);
        } catch (Exception ignored) {}
    }

    public static boolean isEnabled(EntityLivingBase e) {
        String id = e.getEntityData().getString("id"); // often empty on client; fall back to registry
        String key = null;
        if (e.getEntityWorld() != null && e.getEntityWorld().getMinecraftServer() != null) {
            // client side usually doesn't have server; so use registry mapping
        }
        // Use registry mapping via class lookup
        for (EntityEntry ee : ForgeRegistries.ENTITIES) {
            if (ee.getEntityClass().isInstance(e)) {
                key = ee.getRegistryName() == null ? e.getClass().getName() : ee.getRegistryName().toString();
                break;
            }
        }
        if (key == null) key = e.getClass().getName();
        return ENTITY_ENABLED.getOrDefault(key, true);
    }
}