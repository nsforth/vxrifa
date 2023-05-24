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


import io.github.nsforth.vxrifa.VxRifa;
import io.github.nsforth.vxrifa.VxRifaDeliveryOptions;
import io.vertx.core.Future;

/**
 *
 * @author Nikita Staroverov
 */
@VxRifa
public interface SenderReceiverInterface {
    
    void withoutParamsAndReturn();
    
    Future<String> withoutParams();
    
    void withParamsAndWithoutReturn(int param1, String param2);
    
    Future<Integer> withParams(int request);
    
    Future<Void> throwsUnexpectedException(String text);
    
    Future<Void> returnsNullInsteadOfFuture();
    
    Future<Void> ignoredMethod();

    @VxRifaDeliveryOptions(timeout = 500)
    Future<Integer> customDeliveryOptionsMethod(String param, int param2);
}
