package io.github.nsforth.vxrifa;

import io.vertx.core.eventbus.DeliveryOptions;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface VxRifaDeliveryOptions {
    long timeout() default DeliveryOptions.DEFAULT_TIMEOUT;
}
