package io.github.nsforth.vxrifa.test;
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
import io.vertx.core.Handler;
import io.vertx.core.streams.ReadStream;

/**
 *
 * @author Nikita Staroverov <nstaroverov@rtlservice.com>
 */
public class StreamOfStrings implements ReadStream<String> {

    private long counter = 10;
    private Handler<String> handler;
    private Handler<Void> endHandler;
    private Handler<Throwable> exceptionHandler;
    private boolean flowing;
    
    @Override
    public ReadStream<String> exceptionHandler(Handler<Throwable> handler) {
        this.exceptionHandler = handler;      
        return this;
    }

    @Override
    public ReadStream<String> handler(Handler<String> handler) {
        this.handler = handler;
        return this;
    }

    @Override
    public ReadStream<String> pause() {        
        this.flowing = false;        
        return this;
    }

    @Override
    public ReadStream<String> resume() {        
        this.flowing = true;
        while(handle()) {
            
        }
        return this;
    }

    @Override
    public ReadStream<String> endHandler(Handler<Void> endHandler) {
        this.endHandler = endHandler;
        return this;
    }
    
    private boolean handle() {
        if (handler != null) {
            if (flowing) {
                if (counter > 0) {
                    String event = Long.toString(counter--);
                    this.handler.handle(event);
                    return true;
                } else {
                    if (counter == 0) {
                        counter = -1;
                        this.endHandler.handle(null);
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @Override
    public ReadStream<String> fetch(long l) {
        return this;
    }
    
}
