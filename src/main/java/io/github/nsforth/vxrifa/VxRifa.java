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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <h2>VxRIFA - Vert.X Rich Interfaces For Actors</h2><p>
 * This library introduces concept of asynchronous object-oriented programming 'by contract'.<p>
 * Usually if you want to send message in Vert.X from one Actor(Verticle) to other you need to use eventBus.send or eventBus.publish with some object as payload.
 * Objects should be some sort of simple types like String,Integer or special objects like JsonObject.<br>
 * Of course you can register own 'codec' and send anything you want but type checking on receiving side is a developer responsibility.<br>
 * You should also write boiler plate for message handlers and handler registering.<p>
 * VxRifa library implements Java-idiomatic style for actors messaging based on Interfaces. 
 * <h3>How to use library</h3>
 * Any interface can be annotated with {@link VxRifa}. Methods should return void or {@link io.vertx.core.Future}.
 * Whenever java compiler processes annotations (when building project or on the fly in modern IDEs) VxRifa generates special classes that do all work.
 * For example:
 * <pre><code>
 * {@literal @}VxRifa
 * public interface Calculator {
 * 
 *      Future&lt;Double&gt; sumTwoDoubles(Double one, Double two);
 *  
 *      void reset();
 * 
 * }
 * </code></pre>
 * Implementation in Verticle hat exporting such API would look like that:
 * <pre><code>
 * public class CalculatorImpl implements Calculator {
 * 
 *      {@literal @}Override
 *      public Future&lt;Double&gt; sumTwoDoubles(Double one, Double two) {
 *          Future&lt;Double&gt; future = Future.future();
 *          future.complete(one + two);
 *          return future;
 *      }
 *  
 *      {@literal @}Override
 *      public void reset() {
 *          
 *          // Some reset actions 
 *  
 *      }
 * 
 * }
 * </code></pre>
 * Now you can get {@link VxRifaReceiver} with {@link VxRifaUtil#getReceiverRegistrator}
 * which creates any needed eventBus.consumer and calls methods of Calculator whenever it receives messages from other actors.<br>
 * Other Verticles should use {@link VxRifaUtil#getSenderByInterface} that returns VxRifa-driven implementation of Calculator which send messages under the hood.
 * For example:
 * <pre><code>
 * .... CalculatorVerticle ....
 * 
 * Calculator calc_impl = new CalculatorImpl();
 * VxRifaReceiver&lt;Calculator&gt; registrator = VxRifaUtil.getReceiverRegistrator(vertx, Calculator.class);
 * Future&lt;?&gt; when_registered = registrator.registerReceiver(calc_impl);
 * 
 * .... Some other verticles that wants to use Calculator API ....
 * 
 * Calculator calc = VxRifaUtil.getSenderByInterface(vertx, Calculator.class);
 * calc.sumTwoDoubles(1.0, 2.0).setHandler(result -&gt; {
 *      if (result.succeeded()) {
 *          Double sum = result.result();
 *      }
 * });
 * </code></pre>
 * <h3>Notes and limitations</h3>
 * There is one small thing that should be done before using VxRifa. You must call {@link VxRifaUtil#registerRIFACodec(io.vertx.core.Vertx) } once for any instance of Vert.x.<br>
 * VxRifa uses wrapper as container for methods parameters so that wrapper should be registered before any sending by eventBus.<br>
 * You should also remember that any parameters and returned objects should be immutable(effectively immutable) or at least thread-safe.<br>
 * Currently messaging by VxRifa only possible for local non-clustered Vert.X instances because RIFAMessageCodec not supported network encoding/decoding of objects.
 * I hope to solve that in near future.
 * @author Nikita Staroverov
 */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.TYPE)
public @interface VxRifa {
    
}
