/*
 * Copyright (C) 2017 Nikita Staroverov.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301  USA
 */
package io.vxrifa;

import io.vertx.core.Future;

/**
 * Implementor of that class can register some interface annotated with {@link VxRifa} or {@link VxRifaPublish}.
 * After registration vertx consumers accepts messages from eventBus and calls methods of receiver.
 * You can ask {@link #unregisterReceiver} whenever you want. Of course when verticle that calls {@link #registerReceiver} stopped consumers unregistered automatically.
 * @author Nikita Staroverov
 * @param <R> Interface type for receiver generation
 */
public interface VxRifaReceiver<R> {
    
    Future<?> registerReceiver(R receiver);
    
    Future<?> unregisterReceiver();
    
}
