package dev.sealbreaker.gear.gametest;

import dev.sealbreaker.combat.item.SbCombatItems;
import dev.sealbreaker.core.api.component.ReforgePrefix;
import dev.sealbreaker.core.api.component.SbDataComponents;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.GameType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.List;
import java.util.function.Consumer;

/** Game tests for the gear module; instances live in {@code data/sb_gear_tests/test_instance/}, on the shared arena. */
public final class SbGearTestFunctions {
    public static final DeferredRegister<Consumer<GameTestHelper>> TEST_FUNCTIONS =
            DeferredRegister.create(BuiltInRegistries.TEST_FUNCTION, SbGearTests.MOD_ID);

    /** {@code /sb reforge} writes the component, and the component alone drives the name, the tooltip line and the glint. */
    public static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> REFORGE_PREFIX =
            TEST_FUNCTIONS.register("reforge_prefix", () -> SbGearTestFunctions::reforgePrefix);

    private static void reforgePrefix(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.CREATIVE);
        ItemStack sword = new ItemStack(SbCombatItems.SPIKE_SWORD.get());
        player.getInventory().setSelectedSlot(0);
        player.getInventory().setItem(0, sword);
        helper.assertTrue(player.getMainHandItem() == sword, "the mock player must hold the sword");
        helper.assertTrue(!sword.hasFoil() && sword.getHoverName().getString().equals("Spike Sword"), "an unreforged sword is plain");

        // The console source carries every permission; the entity is what the command reforges.
        CommandSourceStack source = helper.getLevel().getServer().createCommandSourceStack().withEntity(player).withSuppressedOutput();
        helper.getLevel().getServer().getCommands().performPrefixedCommand(source, "sb reforge sb_gear:lucky");

        ReforgePrefix prefix = sword.get(SbDataComponents.REFORGE_PREFIX.get());
        helper.assertTrue(prefix != null && prefix.modifier().equals(Identifier.fromNamespaceAndPath("sb_gear", "lucky")),
                "the command must write sb_core:reforge_prefix = sb_gear:lucky on the held item");
        helper.assertTrue(sword.getHoverName().getString().equals("Lucky Spike Sword"),
                "the prefix must lead the name, got '" + sword.getHoverName().getString() + "'");
        helper.assertTrue(sword.hasFoil(), "a reforged GearItem glints");
        List<Component> tooltip = sword.getTooltipLines(Item.TooltipContext.of(helper.getLevel()), player, TooltipFlag.NORMAL);
        helper.assertTrue(tooltip.size() >= 2 && tooltip.get(1).getString().equals("Reforge: Lucky"),
                "the prefix line must follow the name in the tooltip, got " + tooltip.stream().map(Component::getString).toList());

        helper.getLevel().getServer().getCommands().performPrefixedCommand(source, "sb reforge clear");
        helper.assertTrue(!sword.has(SbDataComponents.REFORGE_PREFIX.get()) && !sword.hasFoil()
                && sword.getHoverName().getString().equals("Spike Sword"), "clearing the prefix restores the plain item");
        helper.succeed();
    }

    public static void register(IEventBus modEventBus) {
        TEST_FUNCTIONS.register(modEventBus);
    }

    private SbGearTestFunctions() {
    }
}
