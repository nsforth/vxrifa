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

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import java.lang.reflect.InvocationTargetException;

/**
 *
 * @author Nikita Staroverov
 */
public class VxRifaUtil {
    
    /**
     * Should be called once per Vertx instance before use other methods from {@link VxRifaUtil}
     * @param vertx Vertx instance
     */
    public static void registerRIFACodec(Vertx vertx) {
        vertx.eventBus().registerDefaultCodec(RIFAMessage.class, new RIFAMessageCodec());
        vertx.eventBus().registerDefaultCodec(RIFAReply.class, new RIFAReplyCodec());
    }
    
    /**
     * Returns implementation that can send eventBus messages under the hood like VertX.EventBus.send.
     * Interface should be annotated with {@link VxRifa}.
     * @param <I> Some interface which methods should be wrapped to eventBus messages.
     * @param vertx VertX instance
     * @param interfaceType Class for which implementation should be generated
     * @return Generated sender implementation
     * @throws IllegalArgumentException when sender instance could not be instantiated
     */
    public static <I> I getSenderByInterface(Vertx vertx, Class<I> interfaceType) throws IllegalArgumentException {
        return instantiateSenderImplementation(vertx, interfaceType, null);
    }
    
    /**
     * Same as {@link #getSenderByInterface(io.vertx.core.Vertx, java.lang.Class)} but with possibility to choose other eventBus address.
     * It may be useful when you want to use more than one receivers independent of each other.
     * @param <I> Interface
     * @param vertx Vertx instance
     * @param interfaceType Class for which implementation should be generated
     * @param eventBusAddress Alternate eventBus address, by default address came from interface FQN
     * @return Generated sender implementation
     * @throws IllegalArgumentException when sender instance could not be instantiated
     */
    public static <I> I getSenderByInterface(Vertx vertx, Class<I> interfaceType, String eventBusAddress) throws IllegalArgumentException {        
        return instantiateSenderImplementation(vertx, interfaceType, eventBusAddress);
    }
    
    private static <I> I instantiateSenderImplementation(Vertx vertx, Class<I> interfaceType, String eventBusAddress) {
        assert interfaceType.isInterface();        
        try {
            Class<?> senderClass = ClassLoader.getSystemClassLoader().loadClass(interfaceType.getCanonicalName() + SenderGenerator.VXRIFA_SENDER_SUFFIX);
            try {                                
                I newInstance;
                if (eventBusAddress == null) {
                    newInstance = (I) senderClass.getDeclaredConstructor(Vertx.class).newInstance(vertx);
                } else {
                    newInstance = (I) senderClass.getDeclaredConstructor(Vertx.class, String.class).newInstance(vertx, eventBusAddress);
                }
                return newInstance;
            } catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
                throw new IllegalArgumentException(ex);
            }
        } catch (NoSuchMethodException | SecurityException | ClassNotFoundException ex) {
                throw new IllegalArgumentException(ex);
        }        
    }
    
    /**
     * Same as {@link #getSenderByInterface(io.vertx.core.Vertx, java.lang.Class)} but returns implementation with broadcast eventBus messages like Vertx.eventBus.publish works.
     * @param <I> Interface
     * @param vertx Vertx instance
     * @param interfaceType Class for which implementation should be generated
     * @return Generated publisher implementation
     * @throws IllegalArgumentException when publisher instance could not be instantiated
     */
    public static <I> I getPublisherByInterface(Vertx vertx, Class<I> interfaceType) throws IllegalArgumentException {
        return instantiatePublisherImplementation(vertx, interfaceType, null);
    }
    
    /**
     * Same as {@link #getSenderByInterface(io.vertx.core.Vertx, java.lang.Class, java.lang.String)} but returns implementation with broadcast eventBus messages like Vertx.eventBus.publish works.
     * It may be useful when you want to use more than one receivers independent of each other.
     * @param <I> Interface
     * @param vertx Vertx instance
     * @param interfaceType Class for which implementation should be generated
     * @param eventBusAddress Alternate eventBus address, by default address came from interface FQN
     * @return Generated publisher implementation
     * @throws IllegalArgumentException when publisher instance could not be instantiated
     */
    public static <I> I getPublisherByInterface(Vertx vertx, Class<I> interfaceType, String eventBusAddress) throws IllegalArgumentException {        
        return instantiatePublisherImplementation(vertx, interfaceType, eventBusAddress);
    }
    
    private static <I> I instantiatePublisherImplementation(Vertx vertx, Class<I> interfaceType, String eventBusAddress) {
        assert interfaceType.isInterface();        
        try {
            Class<?> publisherClass = ClassLoader.getSystemClassLoader().loadClass(interfaceType.getCanonicalName() + PublisherGenerator.VXRIFA_PUBLISHER_SUFFIX);
            try {                                
                I newInstance;
                if (eventBusAddress == null) {
                    newInstance = (I) publisherClass.getDeclaredConstructor(Vertx.class).newInstance(vertx);
                } else {
                    newInstance = (I) publisherClass.getDeclaredConstructor(Vertx.class, String.class).newInstance(vertx, eventBusAddress);
                }
                return newInstance;
            } catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
                throw new IllegalArgumentException(ex);
            }
        } catch (NoSuchMethodException | SecurityException | ClassNotFoundException ex) {
                throw new IllegalArgumentException(ex);
        }        
    }
    
    /**
     * Returns object that can register consumer side. Implementation that you get from {@link #getSenderByInterface} or {@link #getPublisherByInterface }
     * sends messages to object registered by {@link VxRifaReceiver}.
     * @param <I> Interface
     * @param vertx Vertx instance
     * @param interfaceType Class for which receiver should be generated
     * @return Generated receiver for registering of your consumer interface implementation
     */
    public static <I> VxRifaReceiver<I> getReceiverRegistrator(Vertx vertx, Class<I> interfaceType) {
        return instantiateReceiverRegistrator(vertx, interfaceType, null);
    }
    
    /**
     * Same as {@link #getReceiverRegistrator} but tries to register receiver and returns future with it
     * @param <I> Interface
     * @param vertx Vertx instance
     * @param interfaceType Class for which receiver should be generated
     * @param receiver Interface implementation that should be registered
     * @return Future that on completion returns successfully registered {@link VxRifaReceiver} and fails otherwise
     */
    public static <I> Future<VxRifaReceiver<I>> registerReceiver(Vertx vertx, Class<I> interfaceType, I receiver) {
        Future<VxRifaReceiver<I>> future = Future.future();
        
        VxRifaReceiver<I> receiverRegistrator = getReceiverRegistrator(vertx, interfaceType);
        receiverRegistrator.registerReceiver(receiver).setHandler(complete -> {
            if (complete.succeeded()) {
                future.complete(receiverRegistrator);
            } else {
                future.fail(complete.cause());
            }
        });
        
        return future;
    }
    
    /**
     * Same as {@link #getReceiverRegistrator(io.vertx.core.Vertx, java.lang.Class) } but you can redefine eventBus address.     
     * @param <I> Interface
     * @param vertx Vertx instance
     * @param interfaceType Class for which receiver should be generated
     * @param eventBusAddress Alternate eventBus address, by default address came from interface FQN
     * @return Generated receiver for registering of your consumer interface implementation
     */
    public static <I> VxRifaReceiver<I> getReceiverRegistrator(Vertx vertx, Class<I> interfaceType, String eventBusAddress) {
        return instantiateReceiverRegistrator(vertx, interfaceType, eventBusAddress);
    }
    
    /**
     * Same as {@link #registerReceiver}  but with possibility to choose alternate eventBus address
     * @param <I> Interface
     * @param vertx Vertx instance
     * @param interfaceType Class for which receiver should be generated
     * @param receiver Interface implementation that should be registered
     * @param eventBusAddress Alternate eventBus address, by default address came from interface FQN
     * @return Future that on completion returns successfully registered {@link VxRifaReceiver} and fails otherwise
     */
    public static <I> Future<VxRifaReceiver<I>> registerReceiver(Vertx vertx, Class<I> interfaceType, I receiver, String eventBusAddress) {
        Future<VxRifaReceiver<I>> future = Future.future();
        
        VxRifaReceiver<I> receiverRegistrator = getReceiverRegistrator(vertx, interfaceType, eventBusAddress);
        receiverRegistrator.registerReceiver(receiver).setHandler(complete -> {
            if (complete.succeeded()) {
                future.complete(receiverRegistrator);
            } else {
                future.fail(complete.cause());
            }
        });
        
        return future;
    }
    
    private static <I> VxRifaReceiver<I> instantiateReceiverRegistrator(Vertx vertx, Class<I> interfaceType, String eventBusAddress) {
        assert interfaceType.isInterface();        
        try {
            Class<VxRifaReceiver<I>> receiverRegistratorClass = (Class<VxRifaReceiver<I>>) ClassLoader.getSystemClassLoader().loadClass(interfaceType.getCanonicalName() + ReceiverGenerator.VXRIFA_RECEIVER_SUFFIX);
            try {                                
                VxRifaReceiver<I> newInstance;
                if (eventBusAddress == null) {
                    newInstance = (VxRifaReceiver<I>) receiverRegistratorClass.getDeclaredConstructor(Vertx.class).newInstance(vertx);
                } else {
                    newInstance = (VxRifaReceiver<I>) receiverRegistratorClass.getDeclaredConstructor(Vertx.class, String.class).newInstance(vertx, eventBusAddress);
                }                
                return newInstance;
            } catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
                throw new IllegalArgumentException(ex);
            }
        } catch (NoSuchMethodException | SecurityException | ClassNotFoundException ex) {
                throw new IllegalArgumentException(ex);
        }
    }
    
}
