package akira.lecovian;

import java.util.Collections;
import java.util.List;

import zone.rong.mixinbooter.IMixinLoader;

public final class LecovianMixinLoader implements IMixinLoader {
    private static final String MIXIN_CONFIG = "mixins.lecovian.json";

    @Override
    public List<String> getMixinConfigs() {
        return Collections.singletonList(MIXIN_CONFIG);
    }

    @Override
    public boolean shouldMixinConfigQueue(String mixinConfig) {
        return !MIXIN_CONFIG.equals(mixinConfig);
    }
}
