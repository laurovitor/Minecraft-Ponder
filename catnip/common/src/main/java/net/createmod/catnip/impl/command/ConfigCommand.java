package net.createmod.catnip.impl.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.createmod.catnip.api.Catnip;
import net.createmod.catnip.impl.config.ConfigHelper;
import net.createmod.catnip.api.config.ConfigId;
import net.createmod.catnip.api.config.ConfigPath;
import net.createmod.catnip.api.config.ConfigSide;
import net.createmod.catnip.api.config.ConfigValueId;
import net.createmod.catnip.api.network.NetworkHelper;
import net.createmod.catnip.impl.config.packet.OpenConfigScreenPacket;
import net.createmod.catnip.impl.config.packet.ClientboundSetConfigValuePacket;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

/// Examples:
/// - `/catnip config` - open Catnip's root config screen
/// - `/catnip config ponder` - open Ponder's root config screen
/// - `/catnip config catnip:client` - open Catnip's config screen with the client config already selected
/// - `/catnip config create:client rainbowDebug set false` - disable Create's rainbow debug for the sender
public class ConfigCommand {
	public static final Component SET_REQUESTED = Component.translatable("command.catnip.config.set.request");

	public static final DynamicCommandExceptionType SET_FAILED = new DynamicCommandExceptionType(
		value -> Component.translatable("command.catnip.config.set.failure", value)
	);

	public static final DynamicCommandExceptionType INVALID_CONFIG = new DynamicCommandExceptionType(
		value -> Component.literal("Invalid config identifier or path: " + value)
	);

	public static ArgumentBuilder<CommandSourceStack, ?> register() {
		return literal("config").executes(withPlayer((_, player) -> {
			NetworkHelper.INSTANCE.sendToClient(player, OpenConfigScreenPacket.forMod(Catnip.ID));
			return Command.SINGLE_SUCCESS;
		})).then(
			argument("target", StringArgumentType.word()).executes(withPlayer((ctx, player) -> {
				String target = StringArgumentType.getString(ctx, "target");
				if (target.contains(":"))
					NetworkHelper.INSTANCE.sendToClient(player, OpenConfigScreenPacket.forConfig(getConfigId(ctx)));
				else
					NetworkHelper.INSTANCE.sendToClient(player, OpenConfigScreenPacket.forMod(target));
				return Command.SINGLE_SUCCESS;
			})).then(
				argument("path", StringArgumentType.word()).executes(withPlayer((ctx, player) -> {
					NetworkHelper.INSTANCE.sendToClient(player, OpenConfigScreenPacket.forValue(getValueId(ctx)));
					return Command.SINGLE_SUCCESS;
				})).then(literal("set").requires(Commands.hasPermission(ConfigHelper.CAN_CHANGE_CONFIGS)).then(
					argument("value", StringArgumentType.string()).executes(ctx -> {
						ConfigValueId valueId = getValueId(ctx);
						String value = StringArgumentType.getString(ctx, "value");

						if (valueId.config().side() == ConfigSide.CLIENT) {
							ServerPlayer player = ctx.getSource().getPlayerOrException();
							NetworkHelper.INSTANCE.sendToClient(player, new ClientboundSetConfigValuePacket(valueId, value));
							ctx.getSource().sendSuccess(() -> SET_REQUESTED, false);
							return Command.SINGLE_SUCCESS;
						}

						Optional<String> error = ConfigHelper.trySetValue(valueId, value);
						if (error.isPresent()) {
							throw SET_FAILED.create(error.get());
						}

						ctx.getSource().sendSuccess(() -> createSetSuccessMessage(valueId, value), true);
						return Command.SINGLE_SUCCESS;
					})
				))
			)
		);
	}

	@FunctionalInterface
	private interface PlayerCommand {
		int run(CommandContext<CommandSourceStack> context, ServerPlayer player) throws CommandSyntaxException;
	}

	private static Command<CommandSourceStack> withPlayer(PlayerCommand command) {
		return ctx -> command.run(ctx, ctx.getSource().getPlayerOrException());
	}

	private static ConfigId getConfigId(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		String target = StringArgumentType.getString(ctx, "target");
		String[] parts = target.split(":", -1);
		try {
			if (parts.length != 2)
				throw new IllegalArgumentException();
			return new ConfigId(parts[0], ConfigSide.valueOf(parts[1].toUpperCase(Locale.ROOT)));
		} catch (IllegalArgumentException ex) {
			throw INVALID_CONFIG.create(target);
		}
	}

	private static ConfigValueId getValueId(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		ConfigId config = getConfigId(ctx);
		String rawPath = StringArgumentType.getString(ctx, "path");
		try {
			return new ConfigValueId(config, new ConfigPath(Arrays.asList(rawPath.split("\\.", -1))));
		} catch (IllegalArgumentException ex) {
			throw INVALID_CONFIG.create(rawPath);
		}
	}

	private static Component createSetSuccessMessage(ConfigValueId value, String serializedValue) {
		return Component.translatableEscape("command.catnip.config.set.success", value, serializedValue);
	}
}
