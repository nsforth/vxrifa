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
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.streams.WriteStream;
import io.vertx.ext.unit.Async;

/**
 *
 * @author Nikita Staroverov <nstaroverov@rtlservice.com>
 */
public class StringsConsumer implements WriteStream<String> {

    private final Async async;
    private final StringBuilder sb;

    StringsConsumer(Async async) {
        this.async = async;
        this.sb = new StringBuilder();
        
    }

    @Override
    public WriteStream<String> exceptionHandler(Handler<Throwable> handler) {
        return this;
    }

    @Override
    public WriteStream<String> write(String data) {
        sb.append(data);        
        async.countDown();
        return this;
    }

    @Override
    public WriteStream<String> write(String data, Handler<AsyncResult<Void>> handler) {
        sb.append(data);
        async.countDown();
        handler.handle(null);
        return this;
    }

    @Override
    public void end() {
        if (sb.toString().equals("123")) {                        
            async.countDown();
        }
    }

    @Override
    public void end(Handler<AsyncResult<Void>> handler) {
        if (sb.toString().equals("123")) {
            async.countDown();
        }
        handler.handle(null);
    }

    @Override
    public void end(String s) {
        this.write(s);
        if (sb.toString().equals("123")) {
            async.countDown();
        }
    }

    @Override
    public WriteStream<String> setWriteQueueMaxSize(int maxSize) {        
        return this;
    }

    @Override
    public boolean writeQueueFull() {
        return false;
    }

    @Override
    public WriteStream<String> drainHandler(Handler<Void> handler) {
        return this;
    }
    
}
