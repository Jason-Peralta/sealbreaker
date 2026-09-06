package dev.sealbreaker.bosses.entity;

import dev.sealbreaker.bosses.SbBosses;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Entity types of the boss module. Milestone 0: the animation spike's dummy. */
@EventBusSubscriber(modid = SbBosses.MOD_ID)
public final class SbBossesEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, SbBosses.MOD_ID);

    public static final DeferredHolder<EntityType<?>, EntityType<SpikeDummy>> SPIKE_DUMMY = ENTITIES.register("spike_dummy",
            () -> EntityType.Builder.of(SpikeDummy::new, MobCategory.MONSTER)
                    .sized(1.0f, 1.5f)
                    .build(ResourceKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath(SbBosses.MOD_ID, "spike_dummy"))));

    public static void register(IEventBus modEventBus) {
        ENTITIES.register(modEventBus);
    }

    @SubscribeEvent
    static void createAttributes(EntityAttributeCreationEvent event) {
        event.put(SPIKE_DUMMY.get(), SpikeDummy.createAttributes().build());
    }

    private SbBossesEntities() {
    }
}
