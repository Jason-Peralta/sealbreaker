package dev.sealbreaker.gear.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import dev.sealbreaker.core.api.component.ReforgePrefix;
import dev.sealbreaker.core.api.component.SbDataComponents;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.IdentifierArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * {@code /sb reforge <modifier>} puts a reforge prefix on the held item and {@code /sb reforge clear} removes it.
 * A gamemaster tool for testing presentation and, later, modifiers; players reforge at the NPC (PRD 3.5).
 * Every module hangs its commands under the shared {@code /sb} root; Brigadier merges the literal.
 */
public final class SbGearCommands {
    public static final String ROOT = "sb";
    private static final SimpleCommandExceptionType EMPTY_HAND =
            new SimpleCommandExceptionType(Component.translatable("commands.sb_gear.reforge.empty_hand"));
    private static final SimpleCommandExceptionType NOT_LIVING =
            new SimpleCommandExceptionType(Component.translatable("commands.sb_gear.reforge.not_living"));

    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal(ROOT)
                .then(Commands.literal("reforge")
                        .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(Commands.literal("clear")
                                .executes(SbGearCommands::clear))
                        .then(Commands.argument("modifier", IdentifierArgument.id())
                                .executes(SbGearCommands::reforge))));
    }

    private static int reforge(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Identifier modifier = IdentifierArgument.getId(context, "modifier");
        ItemStack held = heldItem(context.getSource());
        ReforgePrefix prefix = new ReforgePrefix(modifier);
        held.set(SbDataComponents.REFORGE_PREFIX.get(), prefix);
        context.getSource().sendSuccess(() -> Component.translatable("commands.sb_gear.reforge.applied", prefix.displayName(), held.getHoverName()), true);
        return Command.SINGLE_SUCCESS;
    }

    private static int clear(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ItemStack held = heldItem(context.getSource());
        held.remove(SbDataComponents.REFORGE_PREFIX.get());
        context.getSource().sendSuccess(() -> Component.translatable("commands.sb_gear.reforge.cleared", held.getHoverName()), true);
        return Command.SINGLE_SUCCESS;
    }

    /** The main-hand item of the source entity: a player, or whatever {@code /execute as} chose. */
    private static ItemStack heldItem(CommandSourceStack source) throws CommandSyntaxException {
        Entity entity = source.getEntityOrException();
        if (!(entity instanceof LivingEntity living)) {
            throw NOT_LIVING.create();
        }
        ItemStack held = living.getMainHandItem();
        if (held.isEmpty()) {
            throw EMPTY_HAND.create();
        }
        return held;
    }

    private SbGearCommands() {
    }
}
