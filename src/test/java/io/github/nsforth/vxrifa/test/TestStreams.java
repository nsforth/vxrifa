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

import io.github.nsforth.vxrifa.VxRifaUtil;
import io.vertx.core.streams.WriteStream;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.RunTestOnContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 *
 * @author Nikita Staroverov
 */
@RunWith(VertxUnitRunner.class)
public class TestStreams {

    @Rule
    public final RunTestOnContext rule = new RunTestOnContext();

    @Before
    public void setUp(TestContext context) {
        VxRifaUtil.registerRIFACodec(rule.vertx());
    }
    
    @Test(timeout = 3000L)
    public void testStreams(TestContext context) {     
        Async async = context.async(15);
        rule.vertx().deployVerticle(new StreamOfStringsService(async), deployResult -> {
            if (deployResult.failed()) {
                context.fail(deployResult.cause());
                return;
            }
            StreamService streamService = VxRifaUtil.getSenderByInterface(rule.vertx(), StreamService.class);
            streamService.getStreamOfStrings()
                    .handler(hndlr -> async.countDown())
                    .endHandler(end -> async.countDown());
            WriteStream<String> stringsConsumer = streamService.getStringsConsumer();            
            AtomicInteger counter = new AtomicInteger(0);
            stringsConsumer.drainHandler(hndlr -> {
                while (!stringsConsumer.writeQueueFull()) {
                    int ctr = counter.incrementAndGet();
                    if (ctr < 4) {
                        stringsConsumer.write(String.valueOf(ctr));
                    } else {
                        stringsConsumer.end();
                        break;
                    }
                }
            });
        });
        rule.vertx().setTimer(1000, tid -> {                   
            if (async.count() == 0) {
                async.complete();
            } else {
                context.fail("Some tests failed. Async counter is " + async.count());
            }
        });
    }

}
