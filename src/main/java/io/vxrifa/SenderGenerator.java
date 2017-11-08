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
package io.vxrifa;

import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.TypeVariableName;
import io.vertx.core.Future;
import java.text.MessageFormat;
import javax.annotation.processing.Messager;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;

/**
 * Generates implementation for interface annotated with {@link VxRifa}. Generated implementation wraps method's params to {@link RIFAMessage} and sends message by eventBus.
 * @author Nikita Staroverov <nsforth@gmail.com>
 */
class SenderGenerator {

    static final String VXRIFA_SENDER_SUFFIX = "VxRifaSender";

    private final Messager messager;
    private final TypeElement interfaceElement;

    private FieldSpec vertxField;
    private FieldSpec eventBusAddressField;

    private TypeSpec.Builder classBuilder;

    SenderGenerator(Messager messager, TypeElement interfaceElement) {
        this.messager = messager;
        this.interfaceElement = interfaceElement;
    }

    SenderGenerator generateInitializing() {

        classBuilder = TypeSpec.classBuilder(MessageFormat.format("{0}{1}", interfaceElement.getSimpleName(), VXRIFA_SENDER_SUFFIX)).addModifiers(Modifier.PUBLIC);

        classBuilder.addSuperinterface(TypeName.get(interfaceElement.asType()));

        vertxField = FieldSpec.builder(io.vertx.core.Vertx.class, "vertx", Modifier.PRIVATE, Modifier.FINAL).build();
        classBuilder.addField(vertxField);

        eventBusAddressField = FieldSpec.builder(java.lang.String.class, "eventBusAddress", Modifier.PRIVATE, Modifier.FINAL).build();
        classBuilder.addField(eventBusAddressField);

        classBuilder.addMethod(
                MethodSpec.constructorBuilder()
                        .addModifiers(Modifier.PUBLIC)
                        .addParameter(io.vertx.core.Vertx.class, vertxField.name)
                        .addStatement("this.$N = $N", vertxField, vertxField)
                        .addStatement("this.$N = $S", eventBusAddressField, interfaceElement.getQualifiedName().toString())
                        .build()
        );

        classBuilder.addMethod(
                MethodSpec.constructorBuilder()
                        .addModifiers(Modifier.PUBLIC)
                        .addParameter(io.vertx.core.Vertx.class, vertxField.name)
                        .addParameter(java.lang.String.class, eventBusAddressField.name)
                        .addStatement("this.$N = $N", vertxField, vertxField)
                        .addStatement("this.$N = $N", eventBusAddressField, eventBusAddressField)
                        .build()
        );

        return this;

    }

    SenderGenerator generateMethods() {

        for (Element enclosedElement : interfaceElement.getEnclosedElements()) {

            if (enclosedElement.getKind() == ElementKind.METHOD) {

                ExecutableElement method = (ExecutableElement) enclosedElement;

                if (method.isDefault()) {
                    continue;
                }

                TypeMirror returnType = method.getReturnType();

                messager.printMessage(Diagnostic.Kind.NOTE, MessageFormat.format("Returns: {0} but should {1}", returnType.toString(), Future.class.getCanonicalName()));

                if (!(returnType.toString().startsWith(io.vertx.core.Future.class.getCanonicalName()) || returnType.getKind() == TypeKind.VOID)) {

                    messager.printMessage(Diagnostic.Kind.ERROR, "Methods should return io.vertx.core.Future or void", enclosedElement);
                    continue;

                }

                MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder(method.getSimpleName().toString())
                        .addModifiers(Modifier.PUBLIC)
                        .returns(TypeName.get(returnType));

                MethodsHelper methodsHelper = new MethodsHelper(method);

                methodsHelper.getParameters().forEach(param -> methodBuilder.addParameter(param));

                if (returnType.getKind() == TypeKind.VOID) {
                    if (methodsHelper.getParameters().isEmpty()) {
                        methodBuilder.addStatement("this.$N.eventBus().send($N + $S, null)", vertxField, eventBusAddressField, methodsHelper.generateEventBusSuffix());
                    } else {
                        methodBuilder.addStatement("this.$N.eventBus().send($N + $S, $T.of($N))", vertxField, eventBusAddressField, methodsHelper.generateEventBusSuffix(), RIFAMessage.class, methodsHelper.getParamsNamesCommaSeparated());
                    }
                } else {
                    methodBuilder.addStatement("$T future = $T.future()", TypeName.get(returnType), io.vertx.core.Future.class);
                    if (methodsHelper.getParameters().isEmpty()) {
                        methodBuilder.addStatement("this.$N.eventBus().send($N + $S, null, result -> handle(future,result))", vertxField, eventBusAddressField, methodsHelper.generateEventBusSuffix());
                    } else {
                        methodBuilder.addStatement("this.$N.eventBus().send($N + $S, $T.of($N), result -> handle(future,result))",
                                vertxField, eventBusAddressField, methodsHelper.generateEventBusSuffix(), RIFAMessage.class, methodsHelper.getParamsNamesCommaSeparated()
                        );
                    }
                    methodBuilder.addStatement("return future");
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

        ParameterSpec futureParameter = ParameterSpec.builder(ParameterizedTypeName.get(ClassName.get(io.vertx.core.Future.class), Tvariable), "future").build();
        ParameterSpec asyncResultParameter = ParameterSpec.builder(
                ParameterizedTypeName.get(ClassName.get(io.vertx.core.AsyncResult.class),
                        ParameterizedTypeName.get(ClassName.get(io.vertx.core.eventbus.Message.class), TypeName.get(Object.class))),
                "asyncResult").build();

        handlerBuilder.addParameter(futureParameter)
                .addParameter(asyncResultParameter)
                .beginControlFlow("if ($N.succeeded())", asyncResultParameter)
                .addStatement("$T reply = ($T) $N.result().body()", RIFAMessage.class, RIFAMessage.class, asyncResultParameter)
                .addStatement("$N.complete(($T) reply.get(0))", futureParameter, Tvariable)
                .nextControlFlow("else")
                .addStatement("$N.fail($N.cause().getMessage())", futureParameter, asyncResultParameter)
                .endControlFlow()
                .returns(TypeName.VOID);

        classBuilder.addMethod(handlerBuilder.build());

        return this;

    }

    TypeSpec buildClass() {

        return classBuilder.build();

    }

}
