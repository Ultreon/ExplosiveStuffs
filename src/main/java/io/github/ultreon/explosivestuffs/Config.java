package io.github.ultreon.explosivestuffs;

import com.google.gson.JsonArray;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

// An example config class. This is not required, but it's a good idea to have one to keep your config organized.
// Demonstrates how to use Forge's config APIs
@Mod.EventBusSubscriber(modid = ExplosiveStuffs.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class Config
{
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    public static final ForgeConfigSpec.IntValue IMPACT_RADIUS = BUILDER
            .comment("The impact radius of the orbital strike")
            .defineInRange("orbitalStrike.impactRadius", 10, 1, 50);

    public static final ForgeConfigSpec.DoubleValue RELATIVE_BEAM_SIZE = BUILDER
            .comment("The relative beam size of the orbital strike")
            .defineInRange("orbitalStrike.relativeBeamSize", 1 / 8.0, 1 / 16.0, 1);

    public static final ForgeConfigSpec.BooleanValue TUNNELING = BUILDER
            .comment("Whether the orbital strike should tunnel through blocks")
            .define("orbitalStrike.tunneling", false);

    public static final ForgeConfigSpec.IntValue STRIKE_RANGE = BUILDER
            .comment("Whether the orbital strike should tunnel through blocks")
            .defineInRange("orbitalStrike.strikeRange", 128, 5, 200);

    public static final ForgeConfigSpec.IntValue COOLDOWN = BUILDER
            .comment("How long the orbital strike cooldown should be in ticks")
            .defineInRange("orbitalStrike.cooldown", 100, 10, 200);

    static final ForgeConfigSpec SPEC = BUILDER.build();

    private static boolean validateItemName(final Object obj)
    {
        return obj instanceof final String itemName && ForgeRegistries.ITEMS.containsKey(new ResourceLocation(itemName));
    }

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event)
    {

    }
}
