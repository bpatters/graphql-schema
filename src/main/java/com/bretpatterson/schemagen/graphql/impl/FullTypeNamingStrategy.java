package com.bretpatterson.schemagen.graphql.impl;

import com.bretpatterson.schemagen.graphql.IGraphQLObjectMapper;

import java.lang.reflect.Type;

/**
 * Generates type names using the full package name with . replaced with _
 */
public class FullTypeNamingStrategy extends SimpleTypeNamingStrategy {

	@Override
    public String getTypeName(IGraphQLObjectMapper graphQLObjectMapper, Type type) {
		Class<?> theClass = graphQLObjectMapper.getClassFromType(type);
		String typeName = super.getTypeName(graphQLObjectMapper, type);

		if (theClass.getPackage() != null) {
			return String.format("%s_%s", theClass.getPackage().getName().replace(".", "_"), typeName);
		} else {
			return typeName;
		}
	}
}
