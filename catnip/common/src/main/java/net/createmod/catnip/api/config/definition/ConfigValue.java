package net.createmod.catnip.api.config.definition;

import net.createmod.catnip.api.config.ConfigPath;
import net.createmod.catnip.api.config.validator.Validator;

import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/// A value of a [CatnipConfig].
///
/// Before a config file is loaded, the value holds its declared default. Loading or reloading
/// replaces it with the configured value and notifies change listeners when it differs.
public final class ConfigValue<T> implements ConfigElement {
	public final T defaultValue;
	public final ConfigPath path;
	public final @Nullable Validator<T> validator;

	private final String comment;
	private final List<ChangeCallback<T>> changeCallbacks;

	private T value;

	public ConfigValue(ConfigPath path, String comment, T defaultValue, @Nullable Validator<T> validator) {
		if (defaultValue == null) {
			throw new IllegalStateException("Default value may not be null");
		}


		this.defaultValue = defaultValue;
		this.value = defaultValue;
		this.path = path;
		this.validator = validator;
		this.comment = comment;
		this.changeCallbacks = new ArrayList<>();
	}

	/// @return the current value
	public T get() {
		return this.value;
	}

	/// Set the held value.
	/// @throws IllegalArgumentException if the given value is not [valid][#validate(Object)]
	public void set(T value) {
		Optional<String> message = this.trySet(value);
		if (message.isPresent()) {
			throw new IllegalArgumentException("Invalid value: " + message.get());
		}
	}

	/// Validate the given value.
	/// @return an error message if invalid, otherwise empty
	public Optional<String> validate(T value) {
		if (value == null) {
			return Optional.of("Value may not be null");
		} else if (this.validator == null) {
			return Optional.empty();
		} else {
			return this.validator.test(value);
		}
	}

	/// Attempt to set the current value. May fail if the given value is invalid.
	/// @return an error message if invalid, otherwise empty
	public Optional<String> trySet(T value) {
		Optional<String> message = this.validate(value);
		if (message.isPresent())
			return message;

		if (!value.equals(this.value)) {
			T oldValue = this.value;
			this.value = value;
			this.changeCallbacks.forEach(callback -> callback.afterChange(oldValue, value));
		}

		return Optional.empty();
	}

	/// Register a new [ChangeCallback] to this value, which will be invoked whenever it changes.
	public void registerChangeCallback(ChangeCallback<T> callback) {
		this.changeCallbacks.add(callback);
	}

	@Override
	public String name() {
		return this.path.components.getLast();
	}

	@Override
	public String comment() {
		return this.comment;
	}

	@Override
	public String toString() {
		return this.path.toString();
	}

	@Override
	public boolean equals(Object obj) {
		return obj instanceof ConfigValue<?> that && this.path.equals(that.path);
	}

	@Override
	public int hashCode() {
		return this.path.hashCode();
	}

	@FunctionalInterface
	public interface ChangeCallback<T> {
		/// Invoked after a [ConfigValue] changes, either by being set manually or by being reloaded.
		void afterChange(T oldValue, T newValue);
	}
}
