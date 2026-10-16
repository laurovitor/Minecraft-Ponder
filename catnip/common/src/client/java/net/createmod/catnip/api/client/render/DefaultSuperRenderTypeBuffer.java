package net.createmod.catnip.api.client.render;

import java.util.LinkedHashMap;
import java.util.Map;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.renderer.OrderedSubmitNodeCollector;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;

/** Bridges Catnip's immediate vertex API to Minecraft 26.3's deferred submit-node renderer. */
public class DefaultSuperRenderTypeBuffer implements SuperRenderTypeBuffer {
	private final SubmitNodeCollector submitNodeCollector;
	private final Map<RenderType, RecordingVertexConsumer> earlyBuffer = new LinkedHashMap<>();
	private final Map<RenderType, RecordingVertexConsumer> defaultBuffer = new LinkedHashMap<>();
	private final Map<RenderType, RecordingVertexConsumer> lateBuffer = new LinkedHashMap<>();

	public DefaultSuperRenderTypeBuffer(SubmitNodeCollector submitNodeCollector) {
		this.submitNodeCollector = submitNodeCollector;
	}

	@Override
	public VertexConsumer getEarlyBuffer(RenderType type) {
		return earlyBuffer.computeIfAbsent(type, ignored -> new RecordingVertexConsumer());
	}

	@Override
	public VertexConsumer getBuffer(RenderType type) {
		return defaultBuffer.computeIfAbsent(type, ignored -> new RecordingVertexConsumer());
	}

	@Override
	public VertexConsumer getLateBuffer(RenderType type) {
		return lateBuffer.computeIfAbsent(type, ignored -> new RecordingVertexConsumer());
	}

	@Override
	public void draw() {
		submit(earlyBuffer, submitNodeCollector.order(-1));
		submit(defaultBuffer, submitNodeCollector);
		submit(lateBuffer, submitNodeCollector.order(1));
	}

	@Override
	public void draw(RenderType type) {
		submit(type, earlyBuffer.remove(type), submitNodeCollector.order(-1));
		submit(type, defaultBuffer.remove(type), submitNodeCollector);
		submit(type, lateBuffer.remove(type), submitNodeCollector.order(1));
	}

	@Override
	public SubmitNodeCollector submitNodeCollector() {
		return submitNodeCollector;
	}

	private static void submit(Map<RenderType, RecordingVertexConsumer> recordings, OrderedSubmitNodeCollector collector) {
		recordings.forEach((type, recording) -> submit(type, recording, collector));
		recordings.clear();
	}

	private static void submit(RenderType type, RecordingVertexConsumer recording, OrderedSubmitNodeCollector collector) {
		if (recording == null || recording.isEmpty())
			return;
		collector.submitCustomGeometry(new PoseStack(), type, (pose, target) -> recording.replay(target));
	}
}
