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
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
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
 *
 * @author Nikita Staroverov
 */
class PublisherGenerator {

    static final String VXRIFA_PUBLISHER_SUFFIX = "VxRifaPublisher";

    private final Messager messager;
    private final TypeElement interfaceElement;
    private final Elements elements;

    private FieldSpec vertxField;
    private FieldSpec eventBusAddressField;

    private TypeSpec.Builder tsb;

    PublisherGenerator(Messager messager, TypeElement interfaceElement, Elements elements) {
        this.messager = messager;
        this.interfaceElement = interfaceElement;
        this.elements = elements;
    }

    PublisherGenerator generateInitializing() {

        tsb = GeneratorsHelper.generateClass(interfaceElement, VXRIFA_PUBLISHER_SUFFIX);

        tsb.addSuperinterface(TypeName.get(interfaceElement.asType()));

        vertxField = FieldSpec.builder(io.vertx.core.Vertx.class, "vertx", Modifier.PRIVATE, Modifier.FINAL).build();
        tsb.addField(vertxField);

        eventBusAddressField = FieldSpec.builder(java.lang.String.class, "eventBusAddress", Modifier.PRIVATE, Modifier.FINAL).build();
        tsb.addField(eventBusAddressField);

        tsb.addMethod(
                MethodSpec.constructorBuilder()
                        .addModifiers(Modifier.PUBLIC)
                        .addParameter(io.vertx.core.Vertx.class, vertxField.name)
                        .addStatement("assert $N != null: \"vertx should not be null! May be you try to create publisher not in verticle start?\"", vertxField)
                        .addStatement("this.$N = $N", vertxField, vertxField)
                        .addStatement("this.$N = $S", eventBusAddressField, interfaceElement.getQualifiedName().toString())
                        .build()
        );

        tsb.addMethod(
                MethodSpec.constructorBuilder()
                        .addModifiers(Modifier.PUBLIC)
                        .addParameter(io.vertx.core.Vertx.class, vertxField.name)
                        .addParameter(java.lang.String.class, eventBusAddressField.name)
                        .addStatement("assert $N != null: \"vertx should not be null! May be you try to create publisher not in verticle start?\"", vertxField)
                        .addStatement("this.$N = $N", vertxField, vertxField)
                        .addStatement("this.$N = $N", eventBusAddressField, eventBusAddressField)
                        .build()
        );

        return this;

    }

    PublisherGenerator generateMethods() {

        for (Element enclosedElement : elements.getAllMembers(interfaceElement)) {

            if (GeneratorsHelper.isElementSuitableMethod(enclosedElement)) {

                ExecutableElement method = (ExecutableElement) enclosedElement;

                TypeMirror returnType = method.getReturnType();

                if (returnType.getKind() != TypeKind.VOID) {

                    messager.printMessage(Diagnostic.Kind.ERROR, String.format("%s.%s should return void", interfaceElement, enclosedElement), enclosedElement);
                    continue;

                }

                MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder(method.getSimpleName().toString())
                        .addModifiers(Modifier.PUBLIC)
                        .returns(TypeName.get(returnType));

                MethodsHelper methodsHelper = new MethodsHelper(method);

                methodsHelper.getParameters().forEach(param -> methodBuilder.addParameter(param));

                if (methodsHelper.getParameters().isEmpty()) {
                    methodBuilder.addStatement("this.$N.eventBus().publish($N, $T.of($S, null))", vertxField, eventBusAddressField, RIFAMessage.class, methodsHelper.generateEventBusSuffix());
                } else {
                    methodBuilder.addStatement("this.$N.eventBus().publish($N, $T.of($S, $N))", vertxField, eventBusAddressField, RIFAMessage.class, methodsHelper.generateEventBusSuffix(), methodsHelper.getParamsNamesCommaSeparated());
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
