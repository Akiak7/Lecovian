// src/main/java/zone/rong/mixinbooter/IMixinLoader.java
//
// Compatibility stub for building without depending on MixinBooter directly.
// The real interface is provided at runtime by MixinBooter. The method
// signatures here are kept in sync with the runtime contract so that the
// compiled classes remain binary compatible when the actual library is
// present on the classpath.

package zone.rong.mixinbooter;

import java.util.List;

public interface IMixinLoader {
    List<String> getMixinConfigs();

    default boolean shouldMixinConfigQueue(String mixinConfig) {
        return true;
    }
}
