package net.createmod.catnip.api.client.event;

import com.mojang.blaze3d.vertex.PoseStack;

import net.createmod.catnip.api.event.CatnipEvent;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.LevelRenderState;

/// Invoked at several points during level rendering.
@FunctionalInterface
public interface LevelRenderCallback {
	/// Invoked while custom world geometry is collected.
	CatnipEvent<LevelRenderCallback> COLLECT_SUBMITS = CatnipEvent.create(callbacks -> (renderer, state, transforms, collector) -> {
		for (LevelRenderCallback callback : callbacks) {
			callback.onRender(renderer, state, transforms, collector);
		}
	});

	void onRender(LevelRenderer renderer, LevelRenderState state, PoseStack transforms, SubmitNodeCollector collector);
}
