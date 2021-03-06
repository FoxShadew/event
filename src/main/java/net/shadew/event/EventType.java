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

import java.util.LinkedHashSet;
import java.util.function.Function;

/**
 * A type of event.
 */
public final class EventType<E extends Event> {
    /**
     * An {@link ExceptionHandler} that wraps the exception in an {@link EventException} and throws that.
     */
    public static final ExceptionHandler<Event> THROW_EVENT_EXCEPTIONS = (type, exc) -> {
        throw exc;
    };

    /**
     * An {@link ExceptionHandler} that prints the exception stack trace to {@link System#err}.
     */
    public static final ExceptionHandler<Event> PRINT_EVENT_EXCEPTIONS = (type, exc) -> {
        System.err.println("Exception while invoking event '" + type.getName() + "'");
        exc.printStackTrace(System.err);
    };

    /**
     * An {@link ExceptionHandler} that mutes exceptions.
     */
    public static final ExceptionHandler<Event> SUPPRESS_EVENT_EXCEPTIONS = (type, exc) -> { };

    private final LinkedHashSet<Callback<? super E>> callbacks = new LinkedHashSet<>();
    private final Class<E> cls;
    private final String name;
    private final ExceptionHandler<? super E> excHandler;
    private final boolean cancellable;
    private final boolean propagationStoppable;

    private EventType(Builder<E> builder) {
        this.cls = builder.cls;
        this.name = builder.name;
        this.excHandler = builder.exceptionHandler == null ? THROW_EVENT_EXCEPTIONS : builder.exceptionHandler;
        this.cancellable = builder.cancellable;
        this.propagationStoppable = builder.propagationStoppable;
    }

    /**
     * Triggers this event, calling all callbacks added to this event.
     *
     * @param event The event instance.
     * @return The same event instance given as argument.
     *
     * @throws IllegalArgumentException When the given event was not created for this type.
     */
    public E trigger(E event) {
        if (event.getType() != this)
            throw new IllegalArgumentException("Cannot invoke with event of incorrect type");

        EventException thrownExc = null;

        synchronized (this) {
            for (Callback<? super E> fn : callbacks) {
                try {
                    fn.handle(event);
                } catch (Throwable exc) {
                    try {
                        excHandler.handleException(event, exc);
                    } catch (Throwable e) {
                        if (thrownExc == null) {
                            EventException eventExc;
                            if (e instanceof EventException ee) {
                                eventExc = ee;
                            } else {
                                eventExc = new EventException("Exception while invoking event '" + name + "'", e);
                            }
                            thrownExc = eventExc;
                        } else {
                            thrownExc.addSuppressed(e);
                        }
                    }
                }

                if (!event.mustPropagate()) {
                    break;
                }
            }
        }

        if (thrownExc != null)
            throw thrownExc;

        return event;
    }

    /**
     * Triggers this event, calling all callbacks added to this event.
     *
     * @param eventFactory The factory to create the event instance, for convenience in passing the correct event type
     *                     to the event instance constructor.
     * @return The event instance as constructed by the event factory.
     *
     * @throws IllegalArgumentException When the created event was not created for this type.
     */
    public E trigger(Function<? super EventType<E>, ? extends E> eventFactory) {
        return trigger(eventFactory.apply(this));
    }

    /**
     * Adds a callback to this event. Callbacks are invoked in the order that they are added. If the callback is already
     * added, this method moves the callback to the end of the invocation order.
     *
     * @param callback The callback to add.
     * @throws NullPointerException If the provided callback is null.
     */
    public synchronized void addCallback(Callback<? super E> callback) {
        if (callback == null)
            throw new NullPointerException("Null callback");

        callbacks.remove(callback); // Remove if present so it's added at the end of the linked set
        callbacks.add(callback);
    }

    /**
     * Removes a callback from this event. If the callback is already removed, this method does nothing.
     *
     * @param callback The callback to remove.
     */
    public synchronized void removeCallback(Callback<? super E> callback) {
        callbacks.remove(callback);
    }

    /**
     * Returns the class that all event instances must inherit from when they're created for this event.
     */
    public Class<E> getEventClass() {
        return cls;
    }

    /**
     * Returns the name of this event.
     */
    public String getName() {
        return name;
    }

    /**
     * Returns whether this event is cancellable.
     */
    public boolean isCancellable() {
        return cancellable;
    }

    /**
     * Returns whether other callbacks are invoked after a callback cancels this event.
     */
    public boolean canStopPropagation() {
        return propagationStoppable;
    }

    /**
     * An exception handler handles exceptions that occur during the invocation of a callback.
     */
    public interface ExceptionHandler<E extends Event> {
        /**
         * Handles an exception.
         *
         * @param event The event that occurred.
         * @param exc   The exception that was thrown.
         */
        void handleException(E event, Throwable exc) throws Throwable;
    }

    /**
     * Creates a {@link Builder} for an event type.
     *
     * @param name The name of the event type.
     * @param cls  The class of the event instances accepted by this type.
     * @return The created builder.
     */
    public static <E extends Event> Builder<E> builder(String name, Class<E> cls) {
        return new Builder<>(name, cls);
    }

    /**
     * A builder for an event type.
     */
    public static class Builder<E extends Event> {
        private final String name;
        private final Class<E> cls;

        private boolean cancellable = false;
        private boolean propagationStoppable = false;
        private ExceptionHandler<? super E> exceptionHandler = THROW_EVENT_EXCEPTIONS;

        Builder(String name, Class<E> cls) {
            this.name = name;
            this.cls = cls;
        }

        /**
         * Sets whether events of the event type may be cancelled.
         */
        public Builder<E> cancellable(boolean cancellable) {
            this.cancellable = cancellable;
            return this;
        }

        /**
         * Sets whether a callback may prevent subsequent callbacks from being invoked.
         */
        public Builder<E> canStopPropagation(boolean propagationStoppable) {
            this.propagationStoppable = propagationStoppable;
            return this;
        }

        /**
         * Sets the exception handler for the event type.
         *
         * @throws NullPointerException When the exception handler is null.
         */
        public Builder<E> exceptionHandler(ExceptionHandler<? super E> exceptionHandler) {
            if (exceptionHandler == null)
                throw new NullPointerException("Null event handler");

            this.exceptionHandler = exceptionHandler;
            return this;
        }

        /**
         * Builds the event type.
         */
        public EventType<E> build() {
            return new EventType<>(this);
        }
    }
}
