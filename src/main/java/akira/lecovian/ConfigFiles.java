package akira.lecovian;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraftforge.common.config.ConfigCategory;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;
import net.minecraftforge.fml.common.registry.EntityEntry;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class ConfigFiles {
    private static final String CATEGORY_GENERAL = "all.general";
    private static final String KEY_GLOBAL_SEED = "global_seed";
    private static final String KEY_BLACKLIST = "Entity Resizing Blacklist";
    private static final String KEY_SIGMA = "Entity Sigma Divisors";
    private static final String KEY_BASE_SIGMA_TURNS = "base_sigma_turns";
    private static final String KEY_BASE_SIGMA_STOPS = "base_sigma_stops";
    private static final double DEFAULT_SIGMA_DIVISOR = 3.0D;
    private static final double DEFAULT_BASE_SIGMA_TURNS = 0.30D;
    private static final double DEFAULT_BASE_SIGMA_STOPS = 1.75D;

    public static final class GeneralCfg {
        public long globalSeed = 1337L;
        public double baseSigmaTurns = DEFAULT_BASE_SIGMA_TURNS;
        public double baseSigmaStops = DEFAULT_BASE_SIGMA_STOPS;
    }

    public static GeneralCfg GENERAL = new GeneralCfg();
    public static final Set<String> ENTITY_BLACKLIST = new LinkedHashSet<>();
    public static final Map<String, Double> ENTITY_SIGMA = new LinkedHashMap<>();

    private static Configuration config;

    private ConfigFiles() {}

    public static void loadOrCreate(File configDir) {
        File file = new File(configDir, Lecovian.MODID + ".cfg");
        config = new Configuration(file);
        syncFromDisk();
    }

    private static void syncFromDisk() {
        try {
            config.load();
        } catch (Exception ignored) {}
        initGeneralConfig(config);
        if (config.hasChanged()) config.save();
    }

    public static void rescanEntities() {
        if (config != null) {
            syncFromDisk();
        }
    }

    private static void initGeneralConfig(Configuration cfg) {
        cfg.addCustomCategoryComment(CATEGORY_GENERAL, "General options for Lecovian variants.");

        Property seedProp = cfg.get(CATEGORY_GENERAL, KEY_GLOBAL_SEED, GENERAL.globalSeed,
                "Global seed for deterministic texture variants.", Long.MIN_VALUE, Long.MAX_VALUE);
        GENERAL.globalSeed = seedProp.getLong(GENERAL.globalSeed);

        Property baseSigmaTurnsProp = cfg.get(CATEGORY_GENERAL, KEY_BASE_SIGMA_TURNS, GENERAL.baseSigmaTurns,
                "Base 1σ width for hue variation in turns (1 turn = 360°).", 0.0D, 10.0D);
        GENERAL.baseSigmaTurns = baseSigmaTurnsProp.getDouble(GENERAL.baseSigmaTurns);

        Property baseSigmaStopsProp = cfg.get(CATEGORY_GENERAL, KEY_BASE_SIGMA_STOPS, GENERAL.baseSigmaStops,
                "Base 1σ width for brightness variation in exposure stops.", 0.0D, 20.0D);
        GENERAL.baseSigmaStops = baseSigmaStopsProp.getDouble(GENERAL.baseSigmaStops);

        ENTITY_BLACKLIST.clear();
        Property blacklistProp = cfg.get(CATEGORY_GENERAL, KEY_BLACKLIST, new String[] {},
                "Entities on this list will never receive Lecovian texture variants.");
        String[] blacklist = blacklistProp.getStringList();
        for (String entry : blacklist) {
            String trimmed = entry == null ? "" : entry.trim();
            if (!trimmed.isEmpty()) ENTITY_BLACKLIST.add(trimmed);
        }

        Property sigmaProp = cfg.get(CATEGORY_GENERAL, KEY_SIGMA, new String[] {},
                "Per-entity sigma divisors. FORMAT: <entity id>|<sigma>.");
        List<String> sigmaEntries = new ArrayList<>(Arrays.asList(sigmaProp.getStringList()));

        Map<String, Double> parsed = new LinkedHashMap<>();
        for (String entry : sigmaEntries) {
            int idx = entry.indexOf('|');
            if (idx <= 0) continue;
            String id = entry.substring(0, idx).trim();
            if (id.isEmpty()) continue;
            if (ENTITY_BLACKLIST.contains(id)) continue;
            String value = entry.substring(idx + 1).trim();
            try {
                double sigma = Double.parseDouble(value);
                if (sigma <= 0) sigma = DEFAULT_SIGMA_DIVISOR;
                parsed.put(id, sigma);
            } catch (NumberFormatException ignored) {
            }
        }

        // Ensure all living entities have entries unless blacklisted
        for (EntityEntry ee : ForgeRegistries.ENTITIES) {
            Class<? extends Entity> cls = ee.getEntityClass();
            if (!EntityLivingBase.class.isAssignableFrom(cls)) continue;
            String id = ee.getRegistryName() == null ? cls.getName() : ee.getRegistryName().toString();
            if (ENTITY_BLACKLIST.contains(id)) {
                parsed.remove(id);
                continue;
            }
            parsed.putIfAbsent(id, DEFAULT_SIGMA_DIVISOR);
        }

        ENTITY_SIGMA.clear();
        ENTITY_SIGMA.putAll(parsed);

        // Write back normalized lists for the config file
        List<String> newSigmaList = new ArrayList<>();
        for (Map.Entry<String, Double> e : ENTITY_SIGMA.entrySet()) {
            newSigmaList.add(e.getKey() + "|" + e.getValue());
        }

        ConfigCategory category = cfg.getCategory(CATEGORY_GENERAL);
        List<String> order = new ArrayList<>();
        order.add(KEY_GLOBAL_SEED);
        order.add(KEY_BASE_SIGMA_TURNS);
        order.add(KEY_BASE_SIGMA_STOPS);
        order.add(KEY_BLACKLIST);
        order.add(KEY_SIGMA);
        category.setPropertyOrder(order);

        sigmaProp.set(newSigmaList.toArray(new String[0]));
        blacklistProp.set(ENTITY_BLACKLIST.toArray(new String[0]));
        baseSigmaTurnsProp.set(GENERAL.baseSigmaTurns);
        baseSigmaStopsProp.set(GENERAL.baseSigmaStops);
    }

    public static boolean isEnabled(EntityLivingBase e) {
        String key = getEntityKey(e);
        return !ENTITY_BLACKLIST.contains(key);
    }

    public static double getSigmaFor(EntityLivingBase e) {
        String key = getEntityKey(e);
        return ENTITY_SIGMA.getOrDefault(key, DEFAULT_SIGMA_DIVISOR);
    }

    private static String getEntityKey(EntityLivingBase e) {
        for (EntityEntry ee : ForgeRegistries.ENTITIES) {
            if (ee.getEntityClass().isInstance(e)) {
                return ee.getRegistryName() == null ? e.getClass().getName() : ee.getRegistryName().toString();
            }
        }
        return e.getClass().getName();
    }
}