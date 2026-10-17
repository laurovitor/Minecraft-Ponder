package net.createmod.ponder.impl.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.createmod.ponder.impl.client.tooltip.PonderTooltipHandler;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.CharacterEvent;

@Mixin(EditBox.class)
public class EditBoxMixin {
	@Inject(method = "charTyped(Lnet/minecraft/client/input/CharacterEvent;)Z", at = @At("HEAD"), cancellable = true)
	private void ponder$consumePonderKey(CharacterEvent event, CallbackInfoReturnable<Boolean> cir) {
		if (PonderTooltipHandler.shouldConsumeTypedCharacter()) {
			cir.setReturnValue(true);
		}
	}
}
