package com.bretpatterson.schemagen.graphql.impl;

import com.bretpatterson.schemagen.graphql.IGraphQLObjectMapper;
import com.bretpatterson.schemagen.graphql.annotations.GraphQLName;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * Created by bpatterson on 2/10/16.
 */
public class RelayTypeNamingStrategy extends SimpleTypeNamingStrategy {

    public RelayTypeNamingStrategy(String delimiter, String inputTypePostfix) {
        super(delimiter, inputTypePostfix);
    }

    public RelayTypeNamingStrategy() {
    }

    @Override
    public String getTypeName(IGraphQLObjectMapper graphQLObjectMapper, Type type) {
        String typeString;
        Class<?> theClass = graphQLObjectMapper.getClassFromType(type);

        GraphQLName typeName = theClass.getAnnotation(GraphQLName.class);
        if (typeName != null) {
            return typeName.name();
        }

        // start with the class name
        typeString = theClass.getSimpleName();
        // for parameterized types append the parameter types to build unique type
        if (type instanceof ParameterizedType) {
            ParameterizedType pType = (ParameterizedType) type;

            // relay has special handling for types that end with Connection so we need to
            // preserve the types trailing connection in this scenario
            if (typeString.toLowerCase().endsWith("connection")) {
                // we only want the part of the typeString without the trailing connection
                String prefix = typeString.substring(0, typeString.toLowerCase().lastIndexOf("connection"));
                String suffix = typeString.substring(typeString.toLowerCase().lastIndexOf("connection"), typeString.length());
                // RelayConnection<String> --> Relay_String_Connection
                if (prefix.length() > 0) {
                    typeString = String.format("%s%s%s%s%s",
                            prefix,
                            this.getDelimiter(),
                            getParametersTypeString(graphQLObjectMapper, pType),
                            this.getDelimiter(),
                            suffix);
                } else {
                    typeString = String.format("%s%s%s",
                            getParametersTypeString(graphQLObjectMapper, pType),
                            this.getDelimiter(),
                            suffix);

                }
            } else {
                typeString = String.format("%s%s%s",
                        typeString,
                        this.getDelimiter(),
                        getParametersTypeString(graphQLObjectMapper, pType));
            }
        }

        return typeString;
    }
}
