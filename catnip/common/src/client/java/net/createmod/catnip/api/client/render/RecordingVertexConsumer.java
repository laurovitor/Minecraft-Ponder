package net.createmod.catnip.api.client.render;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import com.mojang.blaze3d.vertex.VertexConsumer;

/** Records vertex-consumer calls so legacy immediate geometry can be replayed through 26.3 submit nodes. */
public class RecordingVertexConsumer implements VertexConsumer {
	private final List<Consumer<VertexConsumer>> operations = new ArrayList<>();

	@Override
	public VertexConsumer addVertex(float x, float y, float z) {
		operations.add(target -> target.addVertex(x, y, z));
		return this;
	}

	@Override
	public VertexConsumer setColor(int r, int g, int b, int a) {
		operations.add(target -> target.setColor(r, g, b, a));
		return this;
	}

	@Override
	public VertexConsumer setColor(int color) {
		operations.add(target -> target.setColor(color));
		return this;
	}

	@Override
	public VertexConsumer setUv(float u, float v) {
		operations.add(target -> target.setUv(u, v));
		return this;
	}

	@Override
	public VertexConsumer setUv1(int u, int v) {
		operations.add(target -> target.setUv1(u, v));
		return this;
	}

	@Override
	public VertexConsumer setUv2(int u, int v) {
		operations.add(target -> target.setUv2(u, v));
		return this;
	}

	@Override
	public VertexConsumer setUv3(float u, float v) {
		operations.add(target -> target.setUv3(u, v));
		return this;
	}

	@Override
	public VertexConsumer setNormal(float x, float y, float z) {
		operations.add(target -> target.setNormal(x, y, z));
		return this;
	}

	@Override
	public VertexConsumer setLineWidth(float width) {
		operations.add(target -> target.setLineWidth(width));
		return this;
	}

	public void replay(VertexConsumer target) {
		operations.forEach(operation -> operation.accept(target));
	}

	public boolean isEmpty() {
		return operations.isEmpty();
	}
}
