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

import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
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
 *
 * @author Nikita Staroverov
 */
class PublisherGenerator {

    static final String VXRIFA_PUBLISHER_SUFFIX = "VxRifaPublisher";

    private final Messager messager;
    private final TypeElement interfaceElement;

    private FieldSpec vertxField;
    private FieldSpec eventBusAddressField;

    private TypeSpec.Builder tsb;

    PublisherGenerator(Messager messager, TypeElement interfaceElement) {
        this.messager = messager;
        this.interfaceElement = interfaceElement;
    }

    PublisherGenerator generateInitializing() {

        tsb = TypeSpec.classBuilder(MessageFormat.format("{0}{1}", interfaceElement.getSimpleName(), VXRIFA_PUBLISHER_SUFFIX)).addModifiers(Modifier.PUBLIC);

        tsb.addSuperinterface(TypeName.get(interfaceElement.asType()));

        vertxField = FieldSpec.builder(io.vertx.core.Vertx.class, "vertx", Modifier.PRIVATE, Modifier.FINAL).build();
        tsb.addField(vertxField);

        eventBusAddressField = FieldSpec.builder(java.lang.String.class, "eventBusAddress", Modifier.PRIVATE, Modifier.FINAL).build();
        tsb.addField(eventBusAddressField);

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

    PublisherGenerator generateMethods() {

        for (Element enclosedElement : interfaceElement.getEnclosedElements()) {

            if (enclosedElement.getKind() == ElementKind.METHOD) {

                ExecutableElement method = (ExecutableElement) enclosedElement;

                if (method.isDefault()) {
                    continue;
                }
                
                if (method.getModifiers().contains(Modifier.STATIC)) {
                    continue;
                }

                TypeMirror returnType = method.getReturnType();

                if (returnType.getKind() != TypeKind.VOID) {

                    messager.printMessage(Diagnostic.Kind.ERROR, "Methods should return void", enclosedElement);
                    continue;

                }

                MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder(method.getSimpleName().toString())
                        .addModifiers(Modifier.PUBLIC)
                        .returns(TypeName.get(returnType));

                MethodsHelper methodsHelper = new MethodsHelper(method);

                methodsHelper.getParameters().forEach(param -> methodBuilder.addParameter(param));

                if (methodsHelper.getParameters().isEmpty()) {
                    methodBuilder.addStatement("this.$N.eventBus().publish($N + $S, null)", vertxField, eventBusAddressField, methodsHelper.generateEventBusSuffix());
                } else {
                    methodBuilder.addStatement("this.$N.eventBus().publish($N + $S, $T.of($N))", vertxField, eventBusAddressField, methodsHelper.generateEventBusSuffix(), RIFAMessage.class, methodsHelper.getParamsNamesCommaSeparated());
                }

                methodBuilder.addAnnotation(Override.class);

                tsb.addMethod(methodBuilder.build());

            }

        }

        return this;

    }

    TypeSpec buildClass() {

        return tsb.build();

    }

}
