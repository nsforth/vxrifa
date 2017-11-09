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

import com.squareup.javapoet.ParameterSpec;
import java.util.ArrayList;
import java.util.List;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;

/**
 *
 * @author Nikita Staroverov
 */
class MethodsHelper {

    private final ExecutableElement method;
    private final List<ParameterSpec> parameters;
    private final String paramsNamesCommaSeparated;
    private final String paramsTypesCommaSeparated;    
    
    MethodsHelper(ExecutableElement method) {

        this.method = method;
        
        parameters = new ArrayList<>();
        
        StringBuilder params_names_comma_separated = new StringBuilder();
        StringBuilder params_types_comma_separated = new StringBuilder();

        for (VariableElement parameter : method.getParameters()) {

            ParameterSpec ps = ParameterSpec.get(parameter);
            
            parameters.add(ps);

            params_names_comma_separated.append(ps.name);
            params_types_comma_separated.append(ps.type);
            params_names_comma_separated.append(",");
            params_types_comma_separated.append(",");

        }

        // Remove trailing comma
        if (params_names_comma_separated.length() > 0) {
            params_names_comma_separated.deleteCharAt(params_names_comma_separated.length() - 1);
        }
        if (params_types_comma_separated.length() > 0) {
            params_types_comma_separated.deleteCharAt(params_types_comma_separated.length() - 1);
        }
        
        paramsNamesCommaSeparated = params_names_comma_separated.toString();
        paramsTypesCommaSeparated = params_types_comma_separated.toString();

    }

    String generateEventBusSuffix() {
        return String.format("::%s(%s)", method.getSimpleName(), paramsTypesCommaSeparated);
    }
    
    List<ParameterSpec> getParameters() {
        return parameters;
    }
    
    String getParamsNamesCommaSeparated() {
        return paramsNamesCommaSeparated;
    }

    String getParamsTypesCommaSeparated() {
        return paramsTypesCommaSeparated;
    }
    
}
