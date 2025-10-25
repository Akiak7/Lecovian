package akira.lecovian;

import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;

import java.util.Map;

@IFMLLoadingPlugin.MCVersion("1.12.2")
public final class LecovianCoreMod implements IFMLLoadingPlugin {

    static {
        System.out.println("[Lecovian] CoreMod loaded (no-op). MixinTweaker should already be active.");
    }

    @Override
    public String[] getASMTransformerClass() {
        return new String[0]; // we rely on mixins instead of raw transformers
    }

    @Override
    public String getModContainerClass() {
        // this is what makes Forge see us as a 'real' mod and fire preInit/loadComplete
        return "akira.lecovian.LecovianModContainer";
    }

    @Override
    public String getSetupClass() {
        return null;
    }

    @Override
    public void injectData(Map<String, Object> data) {
        // not needed
    }

    @Override
    public String getAccessTransformerClass() {
        return null;
    }
}
