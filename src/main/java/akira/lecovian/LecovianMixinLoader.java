// src/main/java/akira/lecovian/LecovianMixinLoader.java
package akira.lecovian;

import zone.rong.mixinbooter.ILateMixinLoader;
import java.util.Collections;
import java.util.List;

public final class LecovianMixinLoader implements ILateMixinLoader {
    @Override public List<String> getMixinConfigs() {
        // Path is relative to resources root
        return Collections.singletonList("mixins.lecovian.json");
    }
    @Override public boolean shouldMixinConfigQueue(String mixinConfig) { return true; }
}
