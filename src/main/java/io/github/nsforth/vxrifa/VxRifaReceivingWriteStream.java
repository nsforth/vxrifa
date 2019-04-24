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
import io.vertx.core.eventbus.Message;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.streams.ReadStream;
import io.vertx.core.streams.WriteStream;

/**
 *
 * @author Nikita Staroverov
 */
public class VxRifaReceivingWriteStream<T> {

    private final Vertx vertx;    
    private final MessageConsumer<RIFAMessage> dataConsumer;
    private final ReadStream<RIFAMessage> dataStream;
    private final WriteStream<T> writeStream;
    private final String controlAddress;     
    private long receivedConter;

    public VxRifaReceivingWriteStream(Vertx vertx, String dataAddress, String controlAddress, Message<RIFAMessage> controlReply, WriteStream<T> writeStream) {
        this.vertx = vertx;
        this.controlAddress = controlAddress;
        this.writeStream = writeStream;
        dataConsumer = vertx.eventBus().consumer(dataAddress, msg -> processDataMessage(msg.body()));
        dataStream = dataConsumer.bodyStream();
        dataStream.pause();
        dataConsumer.completionHandler(result -> {
            if (result.succeeded()) {
                if (!writeStream.writeQueueFull()) {
                    dataStream.resume();              
                }
                writeStream.drainHandler(v -> drainHandler());
                writeStream.exceptionHandler(ex -> excHandler(ex));                
                controlReply.reply(RIFAReply.of(dataAddress));
            } else {
                controlReply.reply(RIFAReply.of(result.cause()));
            }
        });
    }

    private void processDataMessage(RIFAMessage rifaMessage) {
        String messageType = (String) rifaMessage.getParameter(0);
        switch (messageType) {
            case "Data":
                receivedConter++;
                writeStream.write((T) rifaMessage.getParameter(1));
                if (writeStream.writeQueueFull()) {
                    dataStream.pause();
                }
                break;
            case "SetQueueSize":
                writeStream.setWriteQueueMaxSize((int) rifaMessage.getParameter(1));
                if (writeStream.writeQueueFull()) {
                    dataStream.pause();
                } else {
                    dataStream.resume();
                    vertx.eventBus().send(controlAddress, RIFAMessage.of("Ack", receivedConter));
                }
                break;
            case "End":
                T data = (T) rifaMessage.getParameter(1);
                if (data != null) {
                    writeStream.end(data);
                } else {
                    writeStream.end();
                }
                this.dataConsumer.unregister();
                break;
        }
    }

    private void drainHandler() {
        this.dataStream.resume();
        vertx.eventBus().send(controlAddress, RIFAMessage.of("Ack", receivedConter));
    }

    private void excHandler(Throwable ex) {
        vertx.eventBus().send(controlAddress, RIFAMessage.of("Exception", ex));
        this.dataConsumer.unregister();
    }

}
