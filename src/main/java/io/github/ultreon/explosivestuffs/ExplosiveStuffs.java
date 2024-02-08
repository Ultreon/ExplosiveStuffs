package io.github.ultreon.explosivestuffs;

import com.mojang.logging.LogUtils;
import io.github.ultreon.explosivestuffs.client.renderer.OrbitalStrikeRenderer;
import io.github.ultreon.explosivestuffs.entity.OrbitalStrike;
import io.github.ultreon.explosivestuffs.item.OrbitalCanonItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.syncher.EntityDataSerializer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegisterEvent;
import net.minecraftforge.registries.RegistryObject;
import org.slf4j.Logger;
import team.lodestar.lodestone.registry.common.LodestoneFireEffectRegistry;
import team.lodestar.lodestone.systems.fireeffect.FireEffectType;
import team.lodestar.lodestone.systems.particle.type.LodestoneParticleType;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(ExplosiveStuffs.MOD_ID)
public class ExplosiveStuffs {

    // Define mod id in a common place for everything to reference
    public static final String MOD_ID = "explosivestuffs";
    // Directly reference a slf4j logger
    private static final Logger LOGGER = LogUtils.getLogger();
    // Create a Deferred Register to hold Blocks which will all be registered under the "explosive" namespace
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, MOD_ID);
    // Create a Deferred Register to hold Items which will all be registered under the "explosive" namespace
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MOD_ID);
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, MOD_ID);
    public static final DeferredRegister<ParticleType<?>> PARTICLES = DeferredRegister.create(ForgeRegistries.PARTICLE_TYPES, MOD_ID);
    // Create a Deferred Register to hold CreativeModeTabs which will all be registered under the "examplemod" namespace
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MOD_ID);

    public static final RegistryObject<OrbitalCanonItem> ORBITAL_CANON = ITEMS.register("orbital_canon", OrbitalCanonItem::new);
    public static final RegistryObject<EntityType<OrbitalStrike>> ORBITAL_STRIKE = ENTITIES.register("orbital_strike", () -> EntityType.Builder.<OrbitalStrike>of(OrbitalStrike::new, MobCategory.MISC).sized(0.5f, 0.5f).build("orbital_strike"));
    public static RegistryObject<LodestoneParticleType> CIRCLE_PARTICLE = PARTICLES.register("circle", LodestoneParticleType::new);
    public static EntityDataSerializer<Double> DOUBLE_SERIALIZER = EntityDataSerializer.simple(FriendlyByteBuf::writeDouble, FriendlyByteBuf::readDouble);


    // Creates a creative tab with the id "examplemod:example_tab" for the example item, that is placed after the combat tab
    public static final RegistryObject<CreativeModeTab> EXPLOSIVE_STUFFS_TAB = CREATIVE_MODE_TABS.register("explosive_stuffs_tab", () -> CreativeModeTab.builder()
            .withTabsBefore(CreativeModeTabs.COMBAT)
            .icon(() -> ORBITAL_CANON.get().getDefaultInstance())
            .displayItems((parameters, output) -> {
                output.accept(ORBITAL_CANON.get()); // Add the example item to the tab. For your own tabs, this method is preferred over the event
            }).build());
    public static final FireEffectType ORBITAL_STRIKE_EFFECT_TYPE = new FireEffectType("explosive_stuffs:orbital_strikes", 10, 1);

    public ExplosiveStuffs() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        // Register the commonSetup method for modloading
        modEventBus.addListener(this::commonSetup);

        // Register the Deferred Register to the mod event bus so blocks get registered
        BLOCKS.register(modEventBus);
        // Register the Deferred Register to the mod event bus so items get registered
        ITEMS.register(modEventBus);
        // Register the Deferred Register to the mod event bus so entities get registered
        ENTITIES.register(modEventBus);
        // Register the Deferred Register to the mod event bus so particles get registered
        PARTICLES.register(modEventBus);
        // Register the Deferred Register to the mod event bus so tabs get registered
        CREATIVE_MODE_TABS.register(modEventBus);

        // Register ourselves for server and other game events we are interested in
        MinecraftForge.EVENT_BUS.register(this);

        // Register the item to a creative tab
        modEventBus.addListener(this::addCreative);
        modEventBus.addListener(this::onRegister);
        // Register the entity renderers
        modEventBus.addListener(this::registerEntityRenderers);
        // Register the particle factories
        modEventBus.addListener(this::registerParticleFactories);

        // Register miscellaneous things
        modEventBus.addListener(this::onRegister);

        // Register our mod's ForgeConfigSpec so that Forge can create and load the config file for us
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Config.SPEC);

        LodestoneFireEffectRegistry.registerType(ORBITAL_STRIKE_EFFECT_TYPE);
    }

    private void registerEntityRenderers(EntityRenderersEvent event) {
        EntityRenderers.register(ORBITAL_STRIKE.get(), OrbitalStrikeRenderer::new);
    }

    private void registerParticleFactories(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(CIRCLE_PARTICLE.get(), LodestoneParticleType.Factory::new);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        // Some common setup code
        LOGGER.info("HELLO FROM COMMON SETUP");
        LOGGER.info("DIRT BLOCK >> {}", ForgeRegistries.BLOCKS.getKey(Blocks.DIRT));

        if (Config.logDirtBlock)
            LOGGER.info("DIRT BLOCK >> {}", ForgeRegistries.BLOCKS.getKey(Blocks.DIRT));

        LOGGER.info(Config.magicNumberIntroduction + Config.magicNumber);

//        Config.items.forEach((item) -> LOGGER.info("ITEM >> {}", item.toString()));
    }

    // Add the example block item to the building blocks tab
    private void addCreative(BuildCreativeModeTabContentsEvent event) {
    }

    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        // Do something when the server starts
        LOGGER.info("HELLO from server starting");
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

    // You can use EventBusSubscriber to automatically register all static methods in the class annotated with @SubscribeEvent
    @Mod.EventBusSubscriber(modid = MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {

        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            // Some client setup code
            LOGGER.info("HELLO FROM CLIENT SETUP");
            LOGGER.info("MINECRAFT NAME >> {}", Minecraft.getInstance().getUser().getName());
        }
    }
}
