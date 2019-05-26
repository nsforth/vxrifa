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

import io.github.nsforth.vxrifa.VxRifaIgnore;
import io.github.nsforth.vxrifa.VxRifaReceiver;
import io.github.nsforth.vxrifa.VxRifaUtil;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.RunTestOnContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 *
 * @author Nikita Staroverov
 */
@RunWith(VertxUnitRunner.class)
public class TestSimpleSender {

    @Rule
    public final RunTestOnContext rule = new RunTestOnContext();

    class ReceiverVerticle extends AbstractVerticle implements SenderReceiverInterface {

        volatile String withoutParamsAndReturn;
        volatile int param1;
        volatile String param2;
        
        @Override
        public void start(Future<Void> startFuture) throws Exception {
            Future<VxRifaReceiver<SenderReceiverInterface>> registerReceiver = VxRifaUtil.registerReceiver(rule.vertx(), SenderReceiverInterface.class, this);
            registerReceiver.setHandler(handler -> {
                if (handler.succeeded()) {
                    startFuture.complete();
                } else {
                    startFuture.fail(handler.cause());
                }
            });
        }

        @Override
        public void withoutParamsAndReturn() {
            withoutParamsAndReturn = "Passed";
        }

        @Override
        public Future<String> withoutParams() {
            Future<String> future = Future.future();
            future.complete("Passed");
            return future;
        }

        @Override
        public void withParamsAndWithoutReturn(int param1, String param2) {
            this.param1 = param1;
            this.param2 = param2;
        }

        @Override
        public Future<Integer> withParams(int request) {
            Future<Integer> future = Future.future();
            future.complete(request);
            return future;
        }

        @Override
        public Future<Void> throwsUnexpectedException(String text) {
            throw new IllegalStateException(text);
        }        

        @Override
        public Future<Void> returnsNullInsteadOfFuture() {
            return null;
        }

        @Override
        @VxRifaIgnore
        public Future<Void> ignoredMethod() {
            return null;
        }

    }

    @Before
    public void setUp(TestContext context) {
        VxRifaUtil.registerRIFACodec(rule.vertx());
    }

    @Test(timeout = 3000L)
    public void testConstructorAssertions(TestContext context) {
        try {
            SenderReceiverInterface sender = VxRifaUtil.getSenderByInterface(null, SenderReceiverInterface.class);
            context.fail("Sender constructor assertions broken or assertions disabled!");
        } catch (Throwable ex) {
            context.assertEquals(ex.getClass(), IllegalArgumentException.class);
            context.assertEquals(ex.getCause().getClass(), AssertionError.class);
            context.assertEquals(ex.getCause().getMessage(), "vertx should not be null! May be you try to create sender not in verticle start?");
        }
    }
    
    @Test(timeout = 3000L)
    public void testReceiverMethods(TestContext context) {

        Async async = context.async(5);

        ReceiverVerticle receiverVerticle = new ReceiverVerticle();
        SenderReceiverInterface sender = VxRifaUtil.getSenderByInterface(rule.vertx(), SenderReceiverInterface.class);
        rule.vertx().deployVerticle(receiverVerticle, deployResult -> {
            if (deployResult.succeeded()) {
                
                sender.withoutParams().setHandler(result -> {
                    if (result.succeeded()) {
                        context.assertEquals("Passed", result.result());
                        async.countDown();
                    } else {
                        context.fail(result.cause());
                    }
                });
                
                sender.withParams(123).setHandler(result -> {
                    if (result.succeeded()) {
                        context.assertEquals(123, result.result());
                        async.countDown();
                    } else {
                        context.fail(result.cause());
                    }
                });
                
                sender.withParamsAndWithoutReturn(321, "passed");
                
                sender.withoutParamsAndReturn();
                
                sender.throwsUnexpectedException("UnexpectedErrorTest").setHandler(handler -> {
                   if (handler.failed()) {
                       if (handler.cause() instanceof IllegalStateException) {
                           Throwable cause = handler.cause();
                           context.assertEquals(cause.getMessage(), "UnexpectedErrorTest");
                           async.countDown();
                       } else {
                           context.fail("Wrong exception type");
                       }
                   }
                });
                
                sender.returnsNullInsteadOfFuture().setHandler(handler -> {
                   if (handler.failed()) {
                       if (handler.cause() instanceof AssertionError) {
                           Throwable cause = handler.cause();
                           context.assertTrue(cause.getMessage().startsWith("Returned future should not be null! May be you forget to create appropriate result in"));
                           async.countDown();
                       } else {
                           context.fail("Wrong exception type");
                       }
                   } else {
                       context.fail("Wrong exception type");
                   }                    
                });
                
                sender.ignoredMethod().setHandler(handler -> {
                   if (handler.succeeded()) {
                       context.fail("Should catch exception");
                   } else {
                       Throwable cause = handler.cause();
                       if (cause instanceof UnsupportedOperationException) {
                           context.assertEquals(cause.getMessage(), "Method implementation is not provided");
                       } else {
                           context.fail("Wrong exception type");
                       }
                   }
                   async.countDown();
                });
            
            } else {
                context.fail(deployResult.cause());
            }
        });

        rule.vertx().setTimer(1000, tid -> {            
            context.assertEquals(receiverVerticle.param1, 321);
            context.assertEquals(receiverVerticle.param2, "passed");
            context.assertEquals(receiverVerticle.withoutParamsAndReturn, "Passed");
            if (async.count() == 0) {
                async.complete();
            } else {
                context.fail("Not all tests completed!");
            }
        });

    }

}
