package akira.lecovian.math;

import java.util.UUID;

/** Very small, fast, deterministic RNG + Box-Muller normal. */
public final class DeterministicRNG {
    private long x;
    public DeterministicRNG(long seed) { this.x = seed == 0 ? 0x9E3779B97F4A7C15L : seed; }
    public static long seed(UUID u, String base, long global) {
        long s = u.getMostSignificantBits() ^ (u.getLeastSignificantBits() * 0x9E3779B97F4A7C15L);
        s ^= (base == null ? 0 : base.hashCode()) * 0xBF58476D1CE4E5B9L;
        s ^= global * 0x94D049BB133111EBL;
        return s;
    }
    private long nextLong() {
        long z = (x += 0x9E3779B97F4A7C15L);
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        return z ^ (z >>> 31);
    }
    public double nextDouble01() { return (nextLong() >>> 11) * (1.0 / (1L << 53)); }

    /** Standard normal via Box-Muller, deterministic. */
    public double nextGaussian() {
        // Box-Muller using two uniforms
        double u1 = Math.max(1e-12, nextDouble01());
        double u2 = Math.max(1e-12, nextDouble01());
        double r = Math.sqrt(-2.0 * Math.log(u1));
        double theta = 2.0 * Math.PI * u2;
        return r * Math.cos(theta);
    }
}