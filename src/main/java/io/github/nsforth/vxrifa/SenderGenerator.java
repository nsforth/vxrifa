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

import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.TypeVariableName;
import io.vertx.core.Promise;

import java.text.MessageFormat;
import javax.annotation.processing.Messager;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.tools.Diagnostic;

/**
 * Generates implementation for interface annotated with {@link VxRifa}.
 * Generated implementation wraps method's params to {@link RIFAMessage} and
 * sends message by eventBus.
 *
 * @author Nikita Staroverov
 */
class SenderGenerator {

    static final String VXRIFA_SENDER_SUFFIX = "VxRifaSender";

    private final Messager messager;
    private final TypeElement interfaceElement;
    private final Elements elements;

    private FieldSpec vertxField;
    private FieldSpec eventBusAddressField;

    private TypeSpec.Builder classBuilder;

    SenderGenerator(Messager messager, TypeElement interfaceElement, Elements elements) {
        this.messager = messager;
        this.interfaceElement = interfaceElement;
        this.elements = elements;
    }

    SenderGenerator generateInitializing() {

        classBuilder = GeneratorsHelper.generateClass(interfaceElement, VXRIFA_SENDER_SUFFIX);

        classBuilder.addSuperinterface(TypeName.get(interfaceElement.asType()));

        vertxField = FieldSpec.builder(io.vertx.core.Vertx.class, "vertx", Modifier.PRIVATE, Modifier.FINAL).build();
        classBuilder.addField(vertxField);

        eventBusAddressField = FieldSpec.builder(java.lang.String.class, "eventBusAddress", Modifier.PRIVATE, Modifier.FINAL).build();
        classBuilder.addField(eventBusAddressField);

        classBuilder.addMethod(
                MethodSpec.constructorBuilder()
                        .addModifiers(Modifier.PUBLIC)
                        .addParameter(io.vertx.core.Vertx.class, vertxField.name)
                        .addStatement("assert $N != null: \"vertx should not be null! May be you try to create sender not in verticle start?\"", vertxField)
                        .addStatement("this.$N = $N", vertxField, vertxField)
                        .addStatement("this.$N = $S", eventBusAddressField, interfaceElement.getQualifiedName().toString())
                        .build()
        );

        classBuilder.addMethod(
                MethodSpec.constructorBuilder()
                        .addModifiers(Modifier.PUBLIC)
                        .addParameter(io.vertx.core.Vertx.class, vertxField.name)
                        .addParameter(java.lang.String.class, eventBusAddressField.name)
                        .addStatement("assert $N != null: \"vertx should not be null! May be you try to create sender not in verticle start?\"", vertxField)
                        .addStatement("this.$N = $N", vertxField, vertxField)
                        .addStatement("this.$N = $N", eventBusAddressField, eventBusAddressField)
                        .build()
        );

        return this;

    }

    SenderGenerator generateMethods() {

        for (Element enclosedElement : elements.getAllMembers(interfaceElement)) {

            if (GeneratorsHelper.isElementSuitableMethod(enclosedElement)) {

                ExecutableElement method = (ExecutableElement) enclosedElement;

                TypeMirror returnType = method.getReturnType();

                if (!(returnType.toString().startsWith(io.vertx.core.Future.class.getCanonicalName())
                        || returnType.toString().startsWith(io.vertx.core.streams.ReadStream.class.getCanonicalName())
                        || returnType.toString().startsWith(io.vertx.core.streams.WriteStream.class.getCanonicalName())
                        || returnType.getKind() == TypeKind.VOID)) {

                    messager.printMessage(Diagnostic.Kind.ERROR, String.format("%s.%s should return one of io.vertx.core.Future,io.vertx.core.streams.ReadStream,io.vertx.core.streams.WriteStream,void", interfaceElement, enclosedElement), enclosedElement);
                    continue;

                }

                MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder(method.getSimpleName().toString())
                        .addModifiers(Modifier.PUBLIC)
                        .returns(TypeName.get(returnType));

                MethodsHelper methodsHelper = new MethodsHelper(method);

                methodsHelper.getParameters().forEach(param -> methodBuilder.addParameter(param));

                if (returnType.getKind() == TypeKind.VOID) {
                    methodBuilder.addStatement("this.$N.eventBus().send($N, $T.of($S, $N))", vertxField, eventBusAddressField, RIFAMessage.class, methodsHelper.generateEventBusSuffix(), methodsHelper.getParamsNamesCommaSeparatedOrCastedNull());
                } else if (returnType.toString().startsWith(io.vertx.core.streams.ReadStream.class.getCanonicalName())) {
                    methodBuilder.addStatement("String dataAddress = $N + Long.toHexString(java.util.concurrent.ThreadLocalRandom.current().nextLong())", eventBusAddressField);
                    methodBuilder.addStatement("String remoteAddress = $N", eventBusAddressField);
                    methodBuilder.addStatement("return new $T<>($N, dataAddress, remoteAddress, $T.of($S, $N))", VxRifaReceivingReadStream.class, vertxField, RIFAMessage.class, methodsHelper.generateEventBusSuffix(), methodsHelper.getParamsNamesCommaSeparatedOrCastedNull());
                } else if (returnType.toString().startsWith(io.vertx.core.streams.WriteStream.class.getCanonicalName())) {
                    methodBuilder.addStatement("String controlAddress = $N + Long.toHexString(java.util.concurrent.ThreadLocalRandom.current().nextLong())", eventBusAddressField);
                    methodBuilder.addStatement("String remoteAddress = $N", eventBusAddressField);
                    methodBuilder.addStatement("return new $T<>($N, controlAddress, remoteAddress, $T.of($S, $N))", VxRifaSendingWriteStream.class, vertxField, RIFAMessage.class, methodsHelper.generateEventBusSuffix(), methodsHelper.getParamsNamesCommaSeparatedOrCastedNull());
                } else {
                    ParameterizedTypeName parameterizedTypeName = (ParameterizedTypeName) ParameterizedTypeName.get(returnType);
                    TypeName[] typeNames = parameterizedTypeName.typeArguments.toArray(new TypeName[0]);
                    methodBuilder.addStatement("$T promise = $T.promise()", ParameterizedTypeName.get(ClassName.get(io.vertx.core.Promise.class), typeNames), Promise.class);
                    methodBuilder.addStatement("this.$N.eventBus().request($N, $T.of($S, $N), result -> handle(promise,result))",
                            vertxField, eventBusAddressField, RIFAMessage.class, methodsHelper.generateEventBusSuffix(), methodsHelper.getParamsNamesCommaSeparatedOrCastedNull()
                    );
                    methodBuilder.addStatement("return promise.future()");
                }

                methodBuilder.addAnnotation(Override.class);

                classBuilder.addMethod(methodBuilder.build());

            }

        }

        return this;

    }

    SenderGenerator generateHandler() {

        MethodSpec.Builder handlerBuilder = MethodSpec.methodBuilder("handle");

        TypeVariableName Tvariable = TypeVariableName.get("T");

        handlerBuilder.addModifiers(Modifier.PRIVATE)
                .addTypeVariable(Tvariable)
                .addAnnotation(AnnotationSpec.builder(SuppressWarnings.class).addMember("value", "$S", "unchecked").build());

        ParameterSpec promiseParameter = ParameterSpec.builder(ParameterizedTypeName.get(ClassName.get(io.vertx.core.Promise.class), Tvariable), "promise").build();
        ParameterSpec asyncResultParameter = ParameterSpec.builder(
                ParameterizedTypeName.get(ClassName.get(io.vertx.core.AsyncResult.class),
                        ParameterizedTypeName.get(ClassName.get(io.vertx.core.eventbus.Message.class), TypeName.get(Object.class))),
                "asyncResult").build();

        handlerBuilder.addParameter(promiseParameter)
                .addParameter(asyncResultParameter)
                .beginControlFlow("if ($N.succeeded())", asyncResultParameter)
                .addStatement("$T reply = ($T) $N.result().body()", RIFAReply.class, RIFAReply.class, asyncResultParameter)
                .beginControlFlow("if (reply.isExceptional())")
                .addStatement("$N.fail(reply.getException())", promiseParameter)
                .nextControlFlow("else")
                .addStatement("$N.complete(($T) reply.getResult())", promiseParameter, Tvariable)
                .endControlFlow()
                .nextControlFlow("else")
                .addStatement("$N.fail($N.cause().getMessage())", promiseParameter, asyncResultParameter)
                .endControlFlow()
                .returns(TypeName.VOID);

        classBuilder.addMethod(handlerBuilder.build());

        return this;

    }

    TypeSpec buildClass() {

        return classBuilder.build();

    }

}
