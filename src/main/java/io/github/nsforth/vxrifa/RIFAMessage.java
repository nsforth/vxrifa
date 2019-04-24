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
package io.github.nsforth.vxrifa;

/**
 * 
 * @author Nikita Staroverov
 */
public final class RIFAMessage {
    
    private final String suffix;
    private final Object[] payload;

    private RIFAMessage(final String suffix, final Object... payload) {
        this.suffix = suffix;
        this.payload = payload;
    }

    public String getSuffix() {
        return suffix;
    }
    
    public Object getParameter(int index) {
        return payload[index];
    }
    
    public int parametersCount() {
        return payload.length;
    }
    
    public static RIFAMessage of(final String suffix, final Object... payload) {        
        return new RIFAMessage(suffix, payload);
    }
    
}
