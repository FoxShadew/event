/*
 * Copyright 2021 Shadew
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.shadew.event;

/**
 * An instance of an event, to be provided to and, if possible, modified by {@link Callback}s. This class may be
 * overridden to add extra context to the event.
 */
public class Event {
    private final EventType<?> type;
    private boolean cancelled = false;
    private boolean propagationStopped = false;

    /**
     * Constructs an event instance for the given type.
     *
     * @param type The event type, which must accept the class of this event instance.
     * @throws IllegalArgumentException If the given event type does not accept this event instance
     */
    public Event(EventType<?> type) {
        this.type = type;
        if (!type.getEventClass().isAssignableFrom(getClass())) {
            throw new IllegalArgumentException("Event type does not support the event class " + getClass().getName());
        }
    }

    /**
     * Returns true when the name of this event's type matches the given name.
     */
    public final boolean isType(String type) {
        return this.type.getName().equals(type);
    }

    /**
     * Returns true when this event's type matches the given type.
     */
    public final boolean isType(EventType<?> type) {
        return this.type == type;
    }

    /**
     * Calls the given {@link Callback} if this event's type is the given type.
     *
     * @param type     The type to match.
     * @param callback The callback to call.
     */
    public final <E extends Event> void ifType(EventType<? extends E> type, Callback<? super E> callback) throws Throwable {
        if (isType(type))
            callback.handle(type.getEventClass().cast(this));
    }

    /**
     * Runs the given {@link Runnable} if this event's type is the given type.
     *
     * @param type     The type to match.
     * @param callback The runnable to run.
     */
    public final <E extends Event> void ifType(EventType<? extends E> type, Runnable callback) {
        if (isType(type))
            callback.run();
    }

    /**
     * Calls the given {@link Callback} if this event's type has the given name.
     *
     * @param type     The type to match.
     * @param callback The callback to call.
     */
    public final void ifType(String type, Callback<Event> callback) throws Throwable {
        if (isType(type))
            callback.handle(this);
    }

    /**
     * Runs the given {@link Runnable} if this event's type has the given name.
     *
     * @param type     The type to match.
     * @param callback The runnable to run.
     */
    public final void ifType(String type, Runnable callback) {
        if (isType(type))
            callback.run();
    }

    /**
     * Returns the {@link EventType} of this event.
     */
    public final EventType<?> getType() {
        return type;
    }

    /**
     * Returns the name of this event.
     */
    public final String getName() {
        return type.getName();
    }

    /**
     * Sets the cancelled status of this event. This event must be cancellable.
     *
     * @param cancelled The cancelled status.
     * @throws IllegalStateException If this event is not cancellable. Trown even if the cancelled status is attempted
     *                               to be set to false while the event is not cancellable.
     */
    public final void setCancelled(boolean cancelled) {
        if (!type.isCancellable())
            throw new IllegalStateException("Event '" + type.getName() + "' is not cancellable");

        this.cancelled = cancelled;
    }

    /**
     * Returns whether this event is cancellable.
     */
    public final boolean isCancellable() {
        return type.isCancellable();
    }

    /**
     * Returns whether this event is cancelled. When the event is not cancellable, this method returns false by
     * definition.
     */
    public final boolean isCancelled() {
        return cancelled;
    }

    /**
     * Cancels this event, i.e. sets the cancelled status of this event to true. This event must be cancellable.
     *
     * @throws IllegalStateException If this event is not cancellable.
     */
    public final void cancel() {
        setCancelled(true);
    }

    /**
     * Sets the propagation status of this event. This event must be propagation-stoppable.
     *
     * @param propagationStopped The propagation status.
     * @throws IllegalStateException If this event is not propagation-stoppable. Trown even if the propagation status is
     *                               attempted to be set to false while the event is not propagation-stoppable.
     */
    public void setPropagationStopped(boolean propagationStopped) {
        if (!type.canStopPropagation())
            throw new IllegalStateException("Event '" + type.getName() + "' cannot be stopped from propagation");

        this.propagationStopped = propagationStopped;
    }

    /**
     * Returns whether this event's propagation is stopped. When the event is not propagation-stoppable, this method
     * returns false by definition.
     */
    public boolean isPropagationStopped() {
        return propagationStopped;
    }

    /**
     * Returns whether propagation of this event to other callbacks can be stopped.
     */
    public boolean canStopPropagation() {
        return type.canStopPropagation();
    }

    /**
     * Stops propagation of this event to other callbacks. This event must be propagation-stoppable.
     *
     * @throws IllegalStateException If this event is not propagation-stoppable.
     */
    public void stopPropagation() {
        setPropagationStopped(true);
    }

    /**
     * Returns whether subsequent callbacks must still be invoked. By default, this returns always true unless the
     * event's propagation is stopped. This method may be overridden to provide a custom propagation strategy.
     */
    public boolean mustPropagate() {
        if (canStopPropagation())
            return !isPropagationStopped();
        return true;
    }
}
