package akira.lecovian;

import akira.lecovian.img.Filters;
import akira.lecovian.math.DeterministicRNG;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.ResourceLocation;
import net.minecraft.client.resources.IResource;

import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.*;

public final class VariantManager {
    private static final Minecraft MC = Minecraft.getMinecraft();

    /** Cache of built textures keyed by (uuid + baseRL + cfg). LRU-capped to avoid leaks on large packs. */
    private static final int MAX_CACHE = 1024;
    private static final LinkedHashMap<Key, ResourceLocation> CACHE = new LinkedHashMap<Key, ResourceLocation>(64, 0.75f, true) {
        @Override protected boolean removeEldestEntry(Map.Entry<Key, ResourceLocation> eldest) { return size() > MAX_CACHE; }
    };

    private static final Set<Key> BUILDING = Collections.synchronizedSet(new HashSet<>());

    public static ResourceLocation pickOrBuild(EntityLivingBase e, ResourceLocation base) {
        if (!ConfigFiles.isEnabled(e)) return null; // null => use original
        Key key = new Key(e.getUniqueID(), base, ConfigFiles.GENERAL.globalSeed, ConfigFiles.GENERAL.sigmaDivisor);
        synchronized (CACHE) {
            ResourceLocation rl = CACHE.get(key);
            if (rl != null) return rl;
            if (BUILDING.contains(key)) return null; // fall back to original until ready
            BUILDING.add(key);
        }
        try {
            // Deterministic RNG
            long seed = DeterministicRNG.seed(e.getUniqueID(), base.toString(), ConfigFiles.GENERAL.globalSeed);
            DeterministicRNG rng = new DeterministicRNG(seed);

            // Center-heavy amounts derived from a normal -> half-normal -> bounded [0,1)
            double sigmaDiv = Math.max(0.1, ConfigFiles.GENERAL.sigmaDivisor);
double s = Math.max(0.05, ConfigFiles.GENERAL.sigmaDivisor);

// Zero-mean standard normals
double zHue = rng.nextGaussian();
double zVal = rng.nextGaussian();

/* HUE: wrapped normal in "turns" (1 turn = 360°).
   Full range remains possible; σ controls concentration around 0.
   BASE_SIGMA_TURNS is the 1σ width at s=1. Calibrate once. */
final double BASE_SIGMA_TURNS = 0.30;  // ≈36° at s=1
double sigmaTurns = BASE_SIGMA_TURNS / s;
double dHue = zHue * sigmaTurns;       // in turns (can be any real)
dHue -= Math.rint(dHue);               // wrap to (−0.5, 0.5]

// --- Brightness (exposure, in stops) ---
final double BASE_SIGMA_STOPS = 1.75;                 // 1σ ≈ ±1 stop at s=1
double dStops = zVal * (BASE_SIGMA_STOPS / s);       // unbounded RV (no clamp)
float  fStops = (float) dStops;

// Cast for the filter
float fHue = (float) dHue;
//float fVal = (float) dVal;

// (optional) one-shot log
System.out.println("[Lecovian] s=" + s + " hueDeg=" + (fHue*360f) + " stops=" + fStops);


            // Build image now (synchronous). This runs once per entity+texture+cfg.
            IResource res = MC.getResourceManager().getResource(base);
            try (InputStream in = res.getInputStream()) {
                BufferedImage img = TextureUtil.readBufferedImage(in);
                BufferedImage out = Filters.hueBrightnessExposureRGB(img, fHue, fStops);
                DynamicTexture dyn = new DynamicTexture(out);
                ResourceLocation newRL = MC.getTextureManager().getDynamicTextureLocation("lecovian/" + key.shortHash(), dyn);
//                 // TEMP: force an obvious “shiny” 10% to prove pipeline works
// if (rng.nextDouble01() < 0.10) {
//     // Push hue to a noticeable offset, bump brightness
//     BufferedImage shiny = Filters.hueBrightness(img, 0.5f, 0.35f);
//     DynamicTexture dyn2 = new DynamicTexture(shiny);
//     newRL = MC.getTextureManager().getDynamicTextureLocation("lecovian/shiny_" + key.shortHash(), dyn2);
// }
                synchronized (CACHE) { CACHE.put(key, newRL); }
                System.out.println("[Lecovian] built variant for " + base + " -> " + newRL);
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
        final UUID uuid; final String rl; final long seed; final double sigma;
        Key(UUID uuid, ResourceLocation rl, long seed, double sigma) {
            this.uuid = uuid; this.rl = rl.toString(); this.seed = seed; this.sigma = sigma;
        }
        String shortHash() { return Integer.toHexString(hashCode()); }
        @Override public boolean equals(Object o) {
            if (!(o instanceof Key)) return false; Key k = (Key)o;
            return uuid.equals(k.uuid) && rl.equals(k.rl) && seed == k.seed && Double.compare(sigma, k.sigma) == 0;
        }
        @Override public int hashCode() {
            int h = uuid.hashCode(); h = 31*h + rl.hashCode(); h = 31*h + (int)(seed ^ (seed>>>32));
            long t = Double.doubleToLongBits(sigma); h = 31*h + (int)(t ^ (t>>>32)); return h;
        }
    }
}