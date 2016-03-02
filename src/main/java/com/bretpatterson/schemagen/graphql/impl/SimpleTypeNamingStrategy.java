package com.bretpatterson.schemagen.graphql.impl;

import com.bretpatterson.schemagen.graphql.IGraphQLObjectMapper;
import com.bretpatterson.schemagen.graphql.ITypeNamingStrategy;
import com.bretpatterson.schemagen.graphql.annotations.GraphQLTypeName;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * Type naming strategy that uses the simple class name as the GraphQL type.
 */
public class SimpleTypeNamingStrategy implements ITypeNamingStrategy {

	@Override
	public String getTypeName(IGraphQLObjectMapper graphQLObjectMapper, Type type) {
		String typeString;
		Class<?> theClass = graphQLObjectMapper.getClassFromType(type);

		GraphQLTypeName typeName = (GraphQLTypeName) theClass.getAnnotation(GraphQLTypeName.class);
		if (typeName != null) {
			return typeName.name();
		}

		// start with the class name
		typeString = theClass.getSimpleName();

		// for parameterized types append the parameter types to build unique type
		if (type instanceof ParameterizedType) {
			ParameterizedType pType = (ParameterizedType) type;

			typeString += "_"+getParametersTypeString(graphQLObjectMapper, pType);
		}

		return typeString;
	}

	String getParametersTypeString(IGraphQLObjectMapper graphQLObjectMapper, ParameterizedType type) {
		String parametersTypeString = "";
		Type[] subTypes = type.getActualTypeArguments();
		for (int i=0; i< subTypes.length; i++) {
			if (parametersTypeString.length() != 0) {
				parametersTypeString += "_";
			}
			parametersTypeString += graphQLObjectMapper.getClassFromType(subTypes[i]).getSimpleName();
			if (subTypes[i] instanceof ParameterizedType) {
				parametersTypeString += "_"+getParametersTypeString(graphQLObjectMapper, (ParameterizedType) subTypes[i]);
			}
		}

		return parametersTypeString;
	}
}
