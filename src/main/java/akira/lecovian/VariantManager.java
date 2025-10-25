package akira.lecovian;

import akira.lecovian.img.Filters;
import akira.lecovian.math.DeterministicRNG;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraft.client.resources.IResource;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.ResourceLocation;

import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.*;

public final class VariantManager {
    private static final Minecraft MC = Minecraft.getMinecraft();

    /** Cache of built textures keyed by (baseRL + cfg + variant bucket). LRU-capped to avoid leaks on large packs. */
    private static final int MAX_CACHE = 1024;
    private static final LinkedHashMap<Key, ResourceLocation> CACHE = new LinkedHashMap<Key, ResourceLocation>(64, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<Key, ResourceLocation> eldest) {
            return size() > MAX_CACHE;
        }
    };

    private static final Set<Key> BUILDING = Collections.synchronizedSet(new HashSet<>());
    private static final int VARIANT_BUCKETS = 256;

    public static ResourceLocation pickOrBuild(EntityLivingBase e, ResourceLocation base) {
        String entityKey = ConfigFiles.getEntityKey(e);
        if (!ConfigFiles.isEnabled(entityKey)) return null; // null => use original

        int variant = Math.floorMod(e.getUniqueID().hashCode(), VARIANT_BUCKETS);
        double sigma = ConfigFiles.getSigmaFor(entityKey);
        Key key = new Key(base, ConfigFiles.GENERAL.globalSeed, sigma, variant);
        synchronized (CACHE) {
            ResourceLocation rl = CACHE.get(key);
            if (rl != null) return rl;
            if (BUILDING.contains(key)) return null; // fall back to original until ready
            BUILDING.add(key);
        }
        try {
            // Deterministic RNG seeded by base + variant bucket so every client agrees per entity
            String seedKey = base.toString() + "#" + variant;
            long seed = DeterministicRNG.seed(seedKey, ConfigFiles.GENERAL.globalSeed);
            DeterministicRNG rng = new DeterministicRNG(seed);

            double s = Math.max(0.05, sigma);

            // Zero-mean standard normals
            double zHue = rng.nextGaussian();
            double zVal = rng.nextGaussian();

            /* HUE: wrapped normal in "turns" (1 turn = 360°).
               Full range remains possible; σ controls concentration around 0.
               BASE_SIGMA_TURNS is the 1σ width at s=1. Calibrate once. */
double baseSigmaTurns = ConfigFiles.GENERAL.baseSigmaTurns;  // ≈36° at s=1 by default
            double sigmaTurns = baseSigmaTurns / s;
            double dHue = zHue * sigmaTurns;       // in turns (can be any real)
            dHue -= Math.rint(dHue);               // wrap to (−0.5, 0.5]

            // --- Brightness (exposure, in stops) ---
            double baseSigmaStops = ConfigFiles.GENERAL.baseSigmaStops;                 // 1σ ≈ ±1 stop at s=1 by default
            double dStops = zVal * (baseSigmaStops / s);       // unbounded RV (no clamp)
            float fStops = (float) dStops;

            // Cast for the filter
            float fHue = (float) dHue;

            // Build image now (synchronous). This runs once per entity+texture+cfg.
            IResource res = MC.getResourceManager().getResource(base);
            try (InputStream in = res.getInputStream()) {
                BufferedImage img = TextureUtil.readBufferedImage(in);
                BufferedImage out = Filters.hueBrightnessExposureRGB(img, fHue, fStops);
                DynamicTexture dyn = new DynamicTexture(out);
                ResourceLocation newRL = MC.getTextureManager().getDynamicTextureLocation("lecovian/" + key.shortHash(), dyn);
                synchronized (CACHE) { CACHE.put(key, newRL); }
                //System.out.println("[Lecovian] built variant for " + base + " -> " + newRL);
                return newRL;
            }
        } catch (Exception ex) {
            // On failure, fall back to original texture
            return null;
        } finally {
            BUILDING.remove(key);
        }
    }

    public static void clearAll() {
        synchronized (CACHE) { CACHE.clear(); }
        BUILDING.clear();
    }

    private static final class Key {
        final String rl;
        final long seed;
        final double sigma;
        final int variant;

        Key(ResourceLocation rl, long seed, double sigma, int variant) {
            this.rl = rl.toString();
            this.seed = seed;
            this.sigma = sigma;
            this.variant = variant;
        }

        String shortHash() { return Integer.toHexString(hashCode()); }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof Key)) return false;
            Key k = (Key) o;
            return rl.equals(k.rl) && seed == k.seed && Double.compare(sigma, k.sigma) == 0 && variant == k.variant;
        }

        @Override
        public int hashCode() {
            int h = rl.hashCode();
            h = 31 * h + (int) (seed ^ (seed >>> 32));
            long t = Double.doubleToLongBits(sigma);
            h = 31 * h + (int) (t ^ (t >>> 32));
            h = 31 * h + variant;
            return h;
        }
    }
}