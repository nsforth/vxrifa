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

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.WildcardTypeName;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.Message;
import io.vertx.core.eventbus.MessageConsumer;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.processing.Messager;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.util.Elements;

/**
 * Generates delegate class that do mapping between vertx.consumer handler and
 * some class that implements interface annotated with {@link VxRifa} or
 * {@link VxRifaPublish}
 *
 * @author Nikita Staroverov
 */
class ReceiverGenerator {

    static final String VXRIFA_RECEIVER_SUFFIX = "VxRifaReceiver";

    private final Messager messager;
    private final TypeElement interfaceElement;
    private final Elements elements;

    private FieldSpec vertxField;
    private FieldSpec eventBusAddressField;
    private FieldSpec handlersField;
    private FieldSpec consumerField;
    private TypeSpec.Builder tsb;

    ReceiverGenerator(Messager messager, TypeElement interfaceElement, Elements elements) {
        this.messager = messager;
        this.interfaceElement = interfaceElement;
        this.elements = elements;
    }

    ReceiverGenerator generateInitializing() {

        tsb = GeneratorsHelper.generateClass(interfaceElement, VXRIFA_RECEIVER_SUFFIX);

        tsb.addSuperinterface(ParameterizedTypeName.get(ClassName.get(VxRifaReceiver.class), TypeName.get(interfaceElement.asType())));

        vertxField = FieldSpec.builder(io.vertx.core.Vertx.class, "vertx", Modifier.PRIVATE, Modifier.FINAL).build();
        tsb.addField(vertxField);

        eventBusAddressField = FieldSpec.builder(java.lang.String.class, "eventBusAddress", Modifier.PRIVATE, Modifier.FINAL).build();
        tsb.addField(eventBusAddressField);

        handlersField = FieldSpec.builder(ParameterizedTypeName.get(ClassName.get(Map.class), TypeName.get(String.class), ParameterizedTypeName.get(ClassName.get(Handler.class), ParameterizedTypeName.get(ClassName.get(Message.class), ParameterizedTypeName.get(RIFAMessage.class)))), "handlers", Modifier.PRIVATE)
                .build();
        tsb.addField(handlersField);

        consumerField = FieldSpec.builder(ParameterizedTypeName.get(ClassName.get(MessageConsumer.class), TypeName.get(RIFAMessage.class)), "consumer", Modifier.PRIVATE).build();
        tsb.addField(consumerField);

        tsb.addMethod(
                MethodSpec.constructorBuilder()
                        .addModifiers(Modifier.PUBLIC)
                        .addParameter(io.vertx.core.Vertx.class, vertxField.name)
                        .addStatement("assert $N != null: \"vertx should not be null! May be you try to create receiver not in verticle start?\"", vertxField)
                        .addStatement("this.$N = $N", vertxField, vertxField)
                        .addStatement("this.$N = $S", eventBusAddressField, interfaceElement.getQualifiedName().toString())
                        .build()
        );

        tsb.addMethod(
                MethodSpec.constructorBuilder()
                        .addModifiers(Modifier.PUBLIC)
                        .addParameter(io.vertx.core.Vertx.class, vertxField.name)
                        .addParameter(java.lang.String.class, eventBusAddressField.name)
                        .addStatement("assert $N != null: \"vertx should not be null! May be you try to create receiver not in verticle start?\"", vertxField)
                        .addStatement("this.$N = $N", vertxField, vertxField)
                        .addStatement("this.$N = $N", eventBusAddressField, eventBusAddressField)
                        .build()
        );

        return this;

    }

    ReceiverGenerator generateRegisterMethod() {

        MethodSpec.Builder registerMB = MethodSpec.methodBuilder("registerReceiver");

        registerMB.addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .addParameter(TypeName.get(interfaceElement.asType()), "receiver", Modifier.FINAL)
                .returns(ParameterizedTypeName.get(ClassName.get(Future.class), WildcardTypeName.subtypeOf(Object.class)));

        registerMB.addStatement("$N = new $T<>()", handlersField, HashMap.class);

        registerMB.beginControlFlow("try");

        for (Element enclosedElement : elements.getAllMembers(interfaceElement)) {

            if (GeneratorsHelper.isElementSuitableMethod(enclosedElement)) {

                ExecutableElement method = (ExecutableElement) enclosedElement;

                MethodsHelper methodsHelper = new MethodsHelper(method);

                String paramsTypesClassesCommaSeparated = methodsHelper.getParamsTypesClassesCommaSeparated();
                if ("".equals(paramsTypesClassesCommaSeparated)) {
                    registerMB.beginControlFlow("if (receiver.getClass().getMethod($S, (Class<?>[]) null).getAnnotation($T.class) == null)",
                            method.getSimpleName(), TypeName.get(VxRifaIgnore.class)
                    );
                } else {
                    registerMB.beginControlFlow("if (receiver.getClass().getMethod($S, $L).getAnnotation($T.class) == null)",
                            method.getSimpleName(), paramsTypesClassesCommaSeparated, TypeName.get(VxRifaIgnore.class)
                    );
                }
                registerMB.addStatement("$N.put($S, handler -> {$W$L$W})",
                        handlersField,
                        methodsHelper.generateEventBusSuffix(),
                        makeMethodHandler(method).toString()
                );
                registerMB.endControlFlow();

            }
        }
        
        registerMB.addStatement("$N = this.$N.eventBus().consumer($N, message -> $N.getOrDefault(message.body().getSuffix(), msg -> msg.reply($T.of(new UnsupportedOperationException(\"Method implementation is not provided\")))).handle(message))", 
                consumerField, vertxField, eventBusAddressField, handlersField, RIFAReply.class
        );

        registerMB.nextControlFlow("catch ($T ex)", TypeName.get(NoSuchMethodException.class));
        registerMB.addStatement("throw new $T(ex)", TypeName.get(IllegalArgumentException.class));
        registerMB.endControlFlow();
        registerMB.addStatement("$T future = $T.future()", ParameterizedTypeName.get(ClassName.get(Future.class), TypeName.get(Void.class)), Future.class);
        registerMB.addStatement("$N.completionHandler(future.completer())", consumerField);
        registerMB.addStatement("return future");

        tsb.addMethod(registerMB.build());

        return this;

    }

    ReceiverGenerator generateUnregisterMethod() {

        MethodSpec.Builder unregisterMB = MethodSpec.methodBuilder("unregisterReceiver");

        // Generates cosumers waiting Future for success handler unregistration
        unregisterMB.addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .addStatement("$T future = $T.future()", ParameterizedTypeName.get(ClassName.get(Future.class), TypeName.get(Void.class)), Future.class)
                .addStatement("$N.unregister(future.completer())", consumerField)
                .addStatement("return future")
                .returns(ParameterizedTypeName.get(ClassName.get(Future.class), WildcardTypeName.subtypeOf(Object.class)));

        tsb.addMethod(unregisterMB.build());

        return this;

    }

    private CodeBlock makeMethodHandler(ExecutableElement method) {

        CodeBlock.Builder result = CodeBlock.builder();

        // Generates list of params with class casting for example "(String)message.get(0),(Integer)message.get(1)"
        StringBuilder parametersWithCasting = new StringBuilder();
        int parameterNumber = 0;
        for (VariableElement parameter : method.getParameters()) {
            parametersWithCasting.append("(")
                    .append(ParameterSpec.get(parameter).type)
                    .append(")")
                    .append("message.getParameter(")
                    .append(parameterNumber++)
                    .append("),");
        }
        if (parameterNumber > 0) {
            result.addStatement("$T message = handler.body()", RIFAMessage.class);
        }
        // Remove trailing comma
        if (parametersWithCasting.length() > 0) {
            parametersWithCasting.deleteCharAt(parametersWithCasting.length() - 1);
        }

        if (method.getReturnType().getKind() == TypeKind.VOID) {
            result.addStatement("receiver.$L($L)", method.getSimpleName(), parametersWithCasting.toString());
        } else if (method.getReturnType().toString().startsWith(io.vertx.core.streams.ReadStream.class.getCanonicalName())) {
            result
                    .beginControlFlow("try")
                    .addStatement("$T readStream = receiver.$L($L)", TypeName.get(method.getReturnType()), method.getSimpleName(), parametersWithCasting.toString())
                    .addStatement("assert readStream != null: \"Returned ReadStream should not be null! May be you forget to create appropriate result in $L.$L?\"", method.getEnclosingElement(), method.toString())
                    .addStatement("String controlAddress = $N + Long.toHexString(java.util.concurrent.ThreadLocalRandom.current().nextLong())", eventBusAddressField)
                    .addStatement("handler.reply($T.of(controlAddress))", RIFAReply.class)
                    .addStatement("$T vxRifaSendingReadStream = new $T<>($N, handler.headers().get(\"DataAddress\"), controlAddress, readStream)",
                            ParameterizedTypeName.get(ClassName.get(VxRifaSendingReadStream.class), WildcardTypeName.subtypeOf(Object.class)), VxRifaSendingReadStream.class, vertxField)
                    .nextControlFlow("catch (Throwable ex)")
                    .addStatement("handler.reply($T.of(ex))", RIFAReply.class)
                    .endControlFlow();
        } else if (method.getReturnType().toString().startsWith(io.vertx.core.streams.WriteStream.class.getCanonicalName())) {
            result
                    .beginControlFlow("try")
                    .addStatement("$T writeStream = receiver.$L($L)", TypeName.get(method.getReturnType()), method.getSimpleName(), parametersWithCasting.toString())
                    .addStatement("assert writeStream != null: \"Returned WriteStream should not be null! May be you forget to create appropriate result in $L.$L?\"", method.getEnclosingElement(), method.toString())
                    .addStatement("String dataAddress = $N + Long.toHexString(java.util.concurrent.ThreadLocalRandom.current().nextLong())", eventBusAddressField)
                    .addStatement("$T vxRifaReceivingWriteStream = new $T<>($N, dataAddress, handler.headers().get(\"ControlAddress\"), handler, writeStream)",
                            ParameterizedTypeName.get(ClassName.get(VxRifaReceivingWriteStream.class), WildcardTypeName.subtypeOf(Object.class)), VxRifaReceivingWriteStream.class, vertxField)
                    .nextControlFlow("catch (Throwable ex)")
                    .addStatement("handler.reply($T.of(ex))", RIFAReply.class)
                    .endControlFlow();
        } else {
            CodeBlock.Builder lambdaBody = CodeBlock.builder()
                    .indent()
                    .beginControlFlow("if (result.succeeded())")
                    .addStatement("handler.reply($T.of(result.result()))", RIFAReply.class)
                    .nextControlFlow("else")
                    .addStatement("handler.reply($T.of(result.cause()))", RIFAReply.class)
                    .endControlFlow();
            result
                    .beginControlFlow("try")
                    .addStatement("$T returnedFuture = receiver.$L($L)", TypeName.get(method.getReturnType()), method.getSimpleName(), parametersWithCasting.toString())
                    .addStatement("assert returnedFuture != null: \"Returned future should not be null! May be you forget to create appropriate result in $L.$L?\"", method.getEnclosingElement(), method.toString())
                    .addStatement("returnedFuture.setHandler(result -> {\n$W$L\n})", lambdaBody.build().toString())
                    .nextControlFlow("catch (Throwable ex)")
                    .addStatement("handler.reply($T.of(ex))", RIFAReply.class)
                    .endControlFlow();
        }

        return result.build();

    }

    TypeSpec buildClass() {

        return tsb.build();

    }

}
