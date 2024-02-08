package io.github.ultreon.explosivestuffs;

import com.mojang.logging.LogUtils;
import io.github.ultreon.explosivestuffs.client.renderer.OrbitalStrikeRenderer;
import io.github.ultreon.explosivestuffs.entity.OrbitalStrike;
import io.github.ultreon.explosivestuffs.item.OrbitalCannonItem;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataSerializer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegisterEvent;
import net.minecraftforge.registries.RegistryObject;
import org.slf4j.Logger;
import team.lodestar.lodestone.registry.common.LodestoneFireEffectRegistry;
import team.lodestar.lodestone.systems.fireeffect.FireEffectType;
import team.lodestar.lodestone.systems.particle.type.LodestoneParticleType;

@Mod(ExplosiveStuffs.MOD_ID)
public class ExplosiveStuffs {
    public static final String MOD_ID = "explosivestuffs";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, MOD_ID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MOD_ID);
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, MOD_ID);
    public static final DeferredRegister<ParticleType<?>> PARTICLES = DeferredRegister.create(ForgeRegistries.PARTICLE_TYPES, MOD_ID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MOD_ID);

    public static final RegistryObject<OrbitalCannonItem> ORBITAL_CANNON = ITEMS.register("orbital_cannon", OrbitalCannonItem::new);
    public static final RegistryObject<EntityType<OrbitalStrike>> ORBITAL_STRIKE = ENTITIES.register("orbital_strike", () -> EntityType.Builder.<OrbitalStrike>of(OrbitalStrike::new, MobCategory.MISC).sized(0.5f, 0.5f).build("orbital_strike"));
    public static final RegistryObject<LodestoneParticleType> CIRCLE_PARTICLE = PARTICLES.register("circle", LodestoneParticleType::new);

    public static final EntityDataSerializer<Double> DOUBLE_SERIALIZER = EntityDataSerializer.simple(FriendlyByteBuf::writeDouble, FriendlyByteBuf::readDouble);

    public static final FireEffectType ORBITAL_STRIKE_EFFECT_TYPE = new FireEffectType("explosive_stuffs:orbital_strikes", 10, 1);

    public static final RegistryObject<CreativeModeTab> TAB = CREATIVE_MODE_TABS.register("explosive_stuffs_tab", () -> CreativeModeTab.builder()
            .withTabsBefore(CreativeModeTabs.COMBAT)
            .icon(() -> ORBITAL_CANNON.get().getDefaultInstance())
            .title(Component.translatable("itemGroup.explosivestuffs.explosive_stuffs_tab"))
            .displayItems((parameters, output) -> {
                output.accept(ORBITAL_CANNON.get());
            }).build());

    public ExplosiveStuffs() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        LOGGER.info("Hello World from " + MOD_ID);

        // Register the Deferred Register to the mod event bus
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        ENTITIES.register(modEventBus);
        PARTICLES.register(modEventBus);
        CREATIVE_MODE_TABS.register(modEventBus);

        // Register ourselves for server and other game events we are interested in
        MinecraftForge.EVENT_BUS.register(this);

        modEventBus.addListener(this::onRegister);
        modEventBus.addListener(this::registerParticleFactories);

        // Register miscellaneous things
        modEventBus.addListener(this::onRegister);

        // Register our mod's ForgeConfigSpec so that Forge can create and load the config file for us
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Config.SPEC);

        LodestoneFireEffectRegistry.registerType(ORBITAL_STRIKE_EFFECT_TYPE);
    }

    private void registerParticleFactories(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(CIRCLE_PARTICLE.get(), LodestoneParticleType.Factory::new);
    }

    public void onRegister(RegisterEvent event) {
        if (event.getRegistryKey().equals(ForgeRegistries.Keys.ENTITY_DATA_SERIALIZERS)) {
            event.register(ForgeRegistries.Keys.ENTITY_DATA_SERIALIZERS, this::registerEntityDataSerializers);
        }
    }

    private void registerEntityDataSerializers(RegisterEvent.RegisterHelper<EntityDataSerializer<?>> entityDataSerializerRegisterHelper) {
        entityDataSerializerRegisterHelper.register(res("double"), DOUBLE_SERIALIZER);
    }

    private ResourceLocation res(String name) {
        return new ResourceLocation(MOD_ID, name);
    }

    @Mod.EventBusSubscriber(modid = MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {
        @SubscribeEvent
        public static void registerEntityRenderers(EntityRenderersEvent event) {
            EntityRenderers.register(ORBITAL_STRIKE.get(), OrbitalStrikeRenderer::new);
        }
    }
}
