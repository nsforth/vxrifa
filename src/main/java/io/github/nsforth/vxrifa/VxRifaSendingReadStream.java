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

import io.vertx.core.Vertx;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.streams.ReadStream;

/**
 *
 * @author Nikita Staroverov
 */
public class VxRifaSendingReadStream<T> {

    private static final int ACK_WINDOW = 100;

    private final Vertx vertx;
    private final String dataAddress;
    private final MessageConsumer<RIFAMessage> controlConsumer;
    private final ReadStream<T> input;
    private long sentCounter;
    private long ackCounter;    

    public VxRifaSendingReadStream(Vertx vertx, String dataAddress, String controlAddress, ReadStream<T> input) {
        this.vertx = vertx;
        this.dataAddress = dataAddress;       
        this.input = input;
        input.pause();
        input.handler(obj -> sendDataMessage(obj));
        input.endHandler(v -> endHandler());
        input.exceptionHandler(ex -> excHandler(ex));
        controlConsumer = vertx.eventBus().consumer(controlAddress, msg -> receiveControlMessage(msg.body()));
        controlConsumer.completionHandler(result -> {
            if (result.failed()) {
                controlConsumer.unregister();
                input.handler(null);
                input.endHandler(null);
                input.exceptionHandler(null);
                input.resume();
            } else {
                input.resume();
            }
        });
    }

    
    private void sendDataMessage(T obj) {
        sentCounter++;
        vertx.eventBus().send(dataAddress, RIFAMessage.of("Data", obj));
        if (sentCounter - ackCounter >= ACK_WINDOW) {
            input.pause();
        }
    }
    
    private void endHandler() {
        sentCounter++;
        vertx.eventBus().send(dataAddress, RIFAMessage.of("End"));
        controlConsumer.unregister();
    }
    
    private void excHandler(Throwable ex) {
        sentCounter++;
        vertx.eventBus().send(dataAddress, RIFAMessage.of("Exception", ex));
        controlConsumer.unregister();
    }
    
    private void receiveControlMessage(RIFAMessage rifaMessage) {
        String messageType = rifaMessage.getSuffix();
        switch (messageType) {
            case "Ack":
                this.ackCounter = (long) rifaMessage.getParameter(0);
                if (sentCounter - ackCounter < ACK_WINDOW) {                    
                    input.resume();
                }
                break;
        }
    }

}
