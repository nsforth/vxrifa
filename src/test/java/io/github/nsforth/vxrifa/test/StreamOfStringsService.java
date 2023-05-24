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
import io.github.nsforth.vxrifa.VxRifaReceiver;
import io.github.nsforth.vxrifa.VxRifaUtil;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.streams.ReadStream;
import io.vertx.core.streams.WriteStream;
import io.vertx.ext.unit.Async;

/**
 *
 * @author Nikita Staroverov <nstaroverov@rtlservice.com>
 */
class StreamOfStringsService extends AbstractVerticle implements StreamService {

    private final Async async;

    StreamOfStringsService(Async async) {
        this.async = async;
    }

    @Override
    public void start(Promise<Void> startPromise) throws Exception {
        Future<VxRifaReceiver<StreamService>> registerReceiver = VxRifaUtil.registerReceiver(vertx, StreamService.class, this);        
        registerReceiver
                .onSuccess(result -> startPromise.complete())
                .onFailure(e -> startPromise.fail(e));
    }

    @Override
    public ReadStream<String> getStreamOfStrings() {
        return new StreamOfStrings();
    }

    @Override
    public WriteStream<String> getStringsConsumer() {
        return new StringsConsumer(async);
    }
    
}
