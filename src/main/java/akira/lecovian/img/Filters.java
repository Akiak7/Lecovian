package akira.lecovian.img;

import java.awt.*;
import java.awt.image.BufferedImage;

public final class Filters {
    private Filters() {}

    /** Shift hue by dh ([-1..+1] == full circle), adjust value by dv ([-1..+1] additive), leave saturation as-is. */
    public static BufferedImage hueBrightness(BufferedImage src, float dh, float dv) {
        final int w = src.getWidth(), h = src.getHeight();
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int argb = src.getRGB(x, y);
                int a = (argb >>> 24) & 0xFF;
                if (a < 8) { out.setRGB(x, y, argb); continue; }
                int r = (argb >>> 16) & 0xFF, g = (argb >>> 8) & 0xFF, b = argb & 0xFF;
                float[] hsv = Color.RGBtoHSB(r, g, b, null);
                float h1 = hsv[0] + dh; // in [0..1] space; 1 == 360 deg
                h1 = h1 - (float)Math.floor(h1); // wrap
                float s1 = hsv[1];
                float v1 = clamp01(hsv[2] + dv); // additive brightness
                int rgb = Color.HSBtoRGB(h1, s1, v1);
                out.setRGB(x, y, (a << 24) | (rgb & 0xFFFFFF));
            }
        }
        return out;
    }

    public static BufferedImage hueBrightnessExposure(BufferedImage src, float dHueTurns, float dStops) {
    final int w = src.getWidth(), h = src.getHeight();
    BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);

    // sRGB gamma handling. 2.2 is “good enough” for Minecraft textures.
    final double gamma = 2.2;
    final double invGamma = 1.0 / gamma;
    final double mul = Math.pow(2.0, dStops); // exposure factor in linear space

    for (int y = 0; y < h; y++) {
        for (int x = 0; x < w; x++) {
            int argb = src.getRGB(x, y);
            int a = (argb >>> 24) & 0xFF;
            if (a < 8) { out.setRGB(x, y, argb); continue; }

            int r = (argb >>> 16) & 0xFF, g = (argb >>> 8) & 0xFF, b = argb & 0xFF;

            float[] hsv = Color.RGBtoHSB(r, g, b, null);
            float h1 = hsv[0] + dHueTurns;        // dHueTurns is in “turns” (1.0 = 360°)
            h1 = h1 - (float)Math.floor(h1);      // wrap to [0,1)
            float s1 = hsv[1];

            // Convert V to linear, apply exposure, convert back to sRGB.
            double v_lin  = Math.pow(hsv[2], gamma);
            double v_lin2 = v_lin * mul;                      // no clamp on the RV
            double v2     = Math.pow(Math.min(1.0, v_lin2), invGamma); // pixel clamp only
            float  v1     = (float) v2;

            int rgb = Color.HSBtoRGB(h1, s1, v1);
            out.setRGB(x, y, (a << 24) | (rgb & 0xFFFFFF));
        }
    }
    return out;
}

public static BufferedImage hueBrightnessExposureRGB(BufferedImage src, float dHueTurns, float dStops) {
    final int w = src.getWidth(), h = src.getHeight();
    BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);

    final double gamma = 2.2, invGamma = 1.0 / gamma;
    final double mul = Math.pow(2.0, dStops); // +1 stop = ×2 in linear

    for (int y = 0; y < h; y++) {
        for (int x = 0; x < w; x++) {
            int argb = src.getRGB(x, y);
            int a = (argb >>> 24) & 0xFF;
            if (a < 8) { out.setRGB(x, y, argb); continue; }

            int r = (argb >>> 16) & 0xFF, g = (argb >>> 8) & 0xFF, b = argb & 0xFF;

            // rotate hue only (keep original S,V), then apply exposure in RGB
            float[] hsv = Color.RGBtoHSB(r, g, b, null);
            float h1 = hsv[0] + dHueTurns; h1 = h1 - (float)Math.floor(h1);
            int rgbHSB = Color.HSBtoRGB(h1, hsv[1], hsv[2]);

            double sr = ((rgbHSB >> 16) & 0xFF) / 255.0;
            double sg = ((rgbHSB >>  8) & 0xFF) / 255.0;
            double sb = ( rgbHSB        & 0xFF) / 255.0;

            // sRGB -> linear, scale, linear -> sRGB (pixel clamp only)
            sr = Math.pow(sr, gamma); sg = Math.pow(sg, gamma); sb = Math.pow(sb, gamma);
            sr = Math.min(1.0, sr * mul);
            sg = Math.min(1.0, sg * mul);
            sb = Math.min(1.0, sb * mul);
            int R = (int)Math.round(Math.pow(sr, invGamma) * 255.0);
            int G = (int)Math.round(Math.pow(sg, invGamma) * 255.0);
            int B = (int)Math.round(Math.pow(sb, invGamma) * 255.0);

            out.setRGB(x, y, (a << 24) | (R << 16) | (G << 8) | B);
        }
    }
    return out;
}



    private static float clamp01(float v) { return v < 0f ? 0f : (v > 1f ? 1f : v); }
}