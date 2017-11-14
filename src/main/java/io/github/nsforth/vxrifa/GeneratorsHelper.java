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

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;

/**
 *
 * @author Nikita Staroverov
 */
class GeneratorsHelper {
    
    static boolean isElementSuitableMethod(Element enclosedElement) {
        
        if (enclosedElement.getKind() == ElementKind.METHOD) {

                ExecutableElement method = (ExecutableElement) enclosedElement;

                if (method.isDefault()) {
                    return false;
                }               
                if (method.getModifiers().contains(Modifier.STATIC)) {
                    return false;
                }
        }
        
        return true;
        
    }
    
}
