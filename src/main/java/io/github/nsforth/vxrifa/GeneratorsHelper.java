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
import com.squareup.javapoet.TypeSpec;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;

/**
 *
 * @author Nikita Staroverov
 */
class GeneratorsHelper {
    
    static boolean isElementSuitableMethod(Element enclosedElement) {
        
        if (enclosedElement.getKind() == ElementKind.METHOD) {

                ExecutableElement method = (ExecutableElement) enclosedElement;               
                  
                if (!method.getModifiers().contains(Modifier.ABSTRACT)) {
                    return false;
                }                                
                
        }
        
        return true;
        
    }
    
    static TypeSpec.Builder generateClass(TypeElement interfaceElement, String suffix) {
        
        return TypeSpec.classBuilder(String.format("%s%s", interfaceElement.getSimpleName(), suffix))
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(
                        AnnotationSpec.builder(javax.annotation.Generated.class)
                            .addMember("value", "$S", GeneratorsHelper.class.getPackage().getName())
                            .addMember("date", "$S", DateTimeFormatter.ISO_DATE.format(LocalDate.now()))
                        .build()
                );
    }
    
}
