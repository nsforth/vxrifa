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

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.streams.ReadStream;

/**
 *
 * @author Nikita Staroverov
 */
public class VxRifaReceivingReadStream<T> implements ReadStream<T> {

    private static final int ACK_WINDOW = 100;

    private final Vertx vertx;
    private final MessageConsumer<RIFAMessage> dataConsumer;
    private final ReadStream<RIFAMessage> dataStream;
    private String controlAddress;
    private long receivedCounter;
    private long ackCounter;
    private Handler<T> handler;
    private Handler<Throwable> exceptionHandler;
    private Handler<Void> endHandler;

    public VxRifaReceivingReadStream(Vertx vertx, String dataAddress, String remoteAddress, RIFAMessage params) {
        this.vertx = vertx;
        dataConsumer = vertx.eventBus().consumer(dataAddress);
        dataStream = dataConsumer.bodyStream();
        dataStream.handler(msg -> processDataMessage(msg));
        dataConsumer.completionHandler(result -> {
            if (result.failed()) {
                closeExceptionally(result.cause());
            } else {
                vertx.eventBus().request(remoteAddress, params, new DeliveryOptions().addHeader("DataAddress", dataAddress), reply -> {
                    if (reply.succeeded()) {
                        RIFAReply rifaReply = (RIFAReply) reply.result().body();
                        if (rifaReply.isExceptional()) {
                            closeExceptionally(result.cause());
                        } else {
                            controlAddress = (String) rifaReply.getResult();
                        }
                    } else {
                        closeExceptionally(result.cause());
                    }
                });
            }
        });
    }

    @Override
    public ReadStream<T> pause() {
        dataStream.pause();
        return this;
    }

    @Override
    public ReadStream<T> resume() {
        dataStream.resume();
        return this;
    }

    @Override
    public ReadStream<T> handler(Handler<T> handler) {
        this.handler = handler;
        return this;
    }

    @Override
    public ReadStream<T> exceptionHandler(Handler<Throwable> handler) {
        this.exceptionHandler = handler;
        return this;
    }

    @Override
    public ReadStream<T> endHandler(Handler<Void> endHandler) {
        this.endHandler = endHandler;
        return this;
    }

    @SuppressWarnings("unchecked")
    private void processDataMessage(RIFAMessage rifaMessage) {
        receivedCounter++;
        String messageType = rifaMessage.getSuffix();
        switch (messageType) {
            case "Data":
                long acks = receivedCounter - ackCounter;
                if (acks > ACK_WINDOW / 2) {
                    ackCounter = ackCounter + ACK_WINDOW / 2;
                    vertx.eventBus().send(controlAddress, RIFAMessage.of("Ack", ackCounter));
                }
                if (this.handler != null) {
                    this.handler.handle((T) rifaMessage.getParameter(0));
                }
                break;
            case "Exception":
                vertx.eventBus().send(controlAddress, RIFAMessage.of("Ack", receivedCounter));
                closeExceptionally((Throwable) rifaMessage.getParameter(0));
                break;
            case "End":
                vertx.eventBus().send(controlAddress, RIFAMessage.of("Ack", receivedCounter));
                dataConsumer.unregister();
                if (this.endHandler != null) {
                    this.endHandler.handle(null);
                }
                break;
        }
    }

    private void closeExceptionally(Throwable ex) {
        dataConsumer.unregister();
        if (this.exceptionHandler != null) {
            this.exceptionHandler.handle(ex);
        }
    }

    @Override
    public ReadStream<T> fetch(long l) {
        dataStream.fetch(l);
        return this;
    }

}
