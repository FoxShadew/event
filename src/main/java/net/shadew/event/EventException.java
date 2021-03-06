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
 * An exception that can be thrown in {@link EventType.ExceptionHandler}s to wrap a thrown exception. This exception
 * must have a not-null cause.
 */
public class EventException extends RuntimeException {
    public EventException(String message, Throwable cause) {
        super(message, checkNotNull(cause));
    }

    public EventException(Throwable cause) {
        super(checkNotNull(cause));
    }

    private static Throwable checkNotNull(Throwable cause) {
        if (cause == null)
            throw new NullPointerException("Null cause");
        return cause;
    }
}
