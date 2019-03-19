/*
 * Copyright (C) 2019 Nikita Staroverov <nstaroverov@rtlservice.com>.
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
package io.github.nsforth.vxrifa;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Sometimes you do not want to implement all methods, for example you want to get events with {@link VxRifaPublish} Listener but only some of them.
 * You should annotate unwanted methods in your implementation with {@link VxRifaIgnore} and VxRifa skipped generation of eventBus consumers for such methods.<br>
 * {@link VxRifaIgnore} especially useful for Adapter-like classes.
 * Use it with care on {@link VxRifa} interfaces. Invoker receives eventBus timeout when tries to call {@link VxRifaIgnore} methods.
 * @author Nikita Staroverov <nstaroverov@rtlservice.com>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface VxRifaIgnore {
    
}
