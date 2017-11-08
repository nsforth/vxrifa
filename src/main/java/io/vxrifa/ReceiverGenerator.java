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

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.WildcardTypeName;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.eventbus.MessageConsumer;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.processing.Messager;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;

/**
 * Generates delegate class that do mapping between vertx.consumer handler and some class that implements interface annotated with {@link VxRifa} or {@link VxRifaPublish}
 * @author Nikita Staroverov <nsforth@gmail.com>
 */
class ReceiverGenerator {

    static final String VXRIFA_RECEIVER_SUFFIX = "VxRifaReceiver";

    private final Messager messager;
    private final TypeElement interfaceElement;

    private FieldSpec vertxField;
    private FieldSpec eventBusAddressField;
    private FieldSpec consumersField;
    private TypeSpec.Builder tsb;        

    ReceiverGenerator(Messager messager, TypeElement interfaceElement) {
        this.messager = messager;
        this.interfaceElement = interfaceElement;
    }

    ReceiverGenerator generateInitializing() {

        tsb = TypeSpec.classBuilder(MessageFormat.format("{0}{1}", interfaceElement.getSimpleName(), VXRIFA_RECEIVER_SUFFIX)).addModifiers(Modifier.PUBLIC);
        
        tsb.addSuperinterface(ParameterizedTypeName.get(ClassName.get(VxRifaReceiver.class), TypeName.get(interfaceElement.asType())));

        vertxField = FieldSpec.builder(io.vertx.core.Vertx.class, "vertx", Modifier.PRIVATE, Modifier.FINAL).build();
        tsb.addField(vertxField);

        eventBusAddressField = FieldSpec.builder(java.lang.String.class, "eventBusAddress", Modifier.PRIVATE, Modifier.FINAL).build();
        tsb.addField(eventBusAddressField);
        
        consumersField = FieldSpec.builder(ParameterizedTypeName.get(ClassName.get(List.class), ParameterizedTypeName.get(ClassName.get(MessageConsumer.class), WildcardTypeName.subtypeOf(Object.class))), "consumers", Modifier.PRIVATE)
                .build();        
        tsb.addField(consumersField);

        tsb.addMethod(
                MethodSpec.constructorBuilder()
                        .addModifiers(Modifier.PUBLIC)
                        .addParameter(io.vertx.core.Vertx.class, vertxField.name)
                        .addStatement("this.$N = $N", vertxField, vertxField)
                        .addStatement("this.$N = $S", eventBusAddressField, interfaceElement.getQualifiedName().toString())
                        .build()
        );

        tsb.addMethod(
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

    ReceiverGenerator generateRegisterMethod() {

        MethodSpec.Builder registerMB = MethodSpec.methodBuilder("registerReceiver");

        registerMB.addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .addParameter(TypeName.get(interfaceElement.asType()), "receiver", Modifier.FINAL)
                .returns(ParameterizedTypeName.get(ClassName.get(Future.class), WildcardTypeName.subtypeOf(Object.class)));
                
        registerMB.addStatement("$N = new $T<>()", consumersField, ArrayList.class);
        
        for (Element enclosedElement : interfaceElement.getEnclosedElements()) {

            if (enclosedElement.getKind() == ElementKind.METHOD) {

                ExecutableElement method = (ExecutableElement) enclosedElement;

                if (method.isDefault()) {
                    continue;
                }

                MethodsHelper methodsHelper = new MethodsHelper(method);

                registerMB.addStatement("$N.add(this.vertx.eventBus().consumer(eventBusAddress + $S, handler -> {$W$L$W}))",
                        consumersField,
                        methodsHelper.generateEventBusSuffix(),
                        makeMethodHandler(method).toString()
                );

            }
        }
        
        // Generates cosumers waiting Future for success registration
        registerMB.addStatement("return $T.all($N.stream().map((consumer) -> {"
                        + "$T future = $T.future();"
                        + "consumer.completionHandler(future);"
                        + "return future;"                       
                        + "}).collect($T.toList()))",
                        CompositeFuture.class,
                        consumersField,
                        ParameterizedTypeName.get(ClassName.get(Future.class), TypeName.get(Void.class)),
                        TypeName.get(Future.class),
                        TypeName.get(Collectors.class)
                );

        tsb.addMethod(registerMB.build());

        return this;

    }
    
    ReceiverGenerator generateUnregisterMethod() {
        
        MethodSpec.Builder unregisterMB = MethodSpec.methodBuilder("unregisterReceiver");

        // Generates cosumers waiting Future for success handler unregistration
        unregisterMB.addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .addStatement("return $T.all($N.stream().map((consumer) -> {"
                        + "$T future = $T.future();"
                        + "consumer.unregister(future);"
                        + "return future;"                       
                        + "}).collect($T.toList()))",
                        CompositeFuture.class,
                        consumersField,
                        ParameterizedTypeName.get(ClassName.get(Future.class), TypeName.get(Void.class)),
                        TypeName.get(Future.class),
                        TypeName.get(Collectors.class)
                )
                .returns(ParameterizedTypeName.get(ClassName.get(Future.class), WildcardTypeName.subtypeOf(Object.class)));

        tsb.addMethod(unregisterMB.build());
        
        return this;
    
    }

    private CodeBlock makeMethodHandler(ExecutableElement method) {

        CodeBlock.Builder result = CodeBlock.builder();

        result.addStatement("$T message = ($T) handler.body()", RIFAMessage.class, RIFAMessage.class);

        // Generates list of params with class casting for example "(String)message.get(0),(Integer)message.get(1)"
        StringBuilder parametersWithCasting = new StringBuilder();
        int parameterNumber = 0;        
        for (VariableElement parameter : method.getParameters()) {
            parametersWithCasting.append("(")
                .append(ParameterSpec.get(parameter).type)
                .append(")")
                .append("message.get(")
                .append(parameterNumber++)
                .append("),");
        }
        // Remove trailing comma
        if (parametersWithCasting.length() > 0) {
            parametersWithCasting.deleteCharAt(parametersWithCasting.length() - 1);
        }

        if (method.getReturnType().getKind() == TypeKind.VOID) {
            result.addStatement("receiver.$L($L)", method.getSimpleName(), parametersWithCasting.toString());            
        } else {
            CodeBlock.Builder lambdaBody = CodeBlock.builder()
                    .indent()
                    .beginControlFlow("if (result.succeeded())")
                    .addStatement("handler.reply($T.of(result.result()))", RIFAMessage.class)
                    .nextControlFlow("else")
                    .addStatement("handler.fail(1, result.cause().getMessage())")
                    .endControlFlow();            
            result.addStatement("receiver.$L($L).setHandler(result -> {$W$L})", method.getSimpleName(), parametersWithCasting.toString(), lambdaBody.build().toString());
        }

        return result.build();

    }

    TypeSpec buildClass() {

        return tsb.build();

    }

}
