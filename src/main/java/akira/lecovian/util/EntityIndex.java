// src/main/java/akira/lecovian/util/EntityIndex.java
package akira.lecovian.util;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.EntityEntry;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class EntityIndex {

    public static List<String> collectLivingEntityIds() {
        List<String> out = new ArrayList<>(ForgeRegistries.ENTITIES.getValuesCollection().size());
        for (EntityEntry entry : ForgeRegistries.ENTITIES.getValuesCollection()) {
            Class<? extends Entity> cls = entry.getEntityClass();
            if (!EntityLivingBase.class.isAssignableFrom(cls)) continue;         // we only care about living mobs
            if (Modifier.isAbstract(cls.getModifiers())) continue;               // skip abstract base classes
            ResourceLocation id = entry.getRegistryName();
            if (id == null) continue;
            out.add(id.toString());                                              // e.g., "lycanitesmobs:behemoth"
        }
        out.sort(Comparator.naturalOrder());
        return out;
    }
}
