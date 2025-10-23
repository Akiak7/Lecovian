package akira.lecovian;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.client.event.TextureStitchEvent;

@Mod(modid = Lecovian.MODID, name = "Lecovian", version = "1.0.0", clientSideOnly = true)
public class Lecovian {
    public static final String MODID = "lecovian";

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent e) {
        ConfigFiles.loadOrCreate(e.getModConfigurationDirectory());
        MinecraftForge.EVENT_BUS.register(this);
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent e) {
        // Nothing else required; mixin does the hook.
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onTextureStitch(TextureStitchEvent.Post ev) {
        // Optional: clear caches on atlas rebuilds (resource reloads).
        VariantManager.clearAll();
    }
}