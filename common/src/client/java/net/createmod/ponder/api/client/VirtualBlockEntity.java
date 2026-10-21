package net.createmod.ponder.api.client;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;

/**
 * Used for simulating BE's in a client-only setting (like Ponder)
 */
public interface VirtualBlockEntity {

	void markVirtual();

	boolean isVirtual();

	default void readPonderClientData(CompoundTag tag, HolderLookup.Provider registries) {
	}

}
