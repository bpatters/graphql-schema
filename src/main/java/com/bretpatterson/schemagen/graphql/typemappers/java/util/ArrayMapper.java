package com.bretpatterson.schemagen.graphql.typemappers.java.util;

import com.bretpatterson.schemagen.graphql.IGraphQLObjectMapper;
import com.bretpatterson.schemagen.graphql.annotations.GraphQLTypeMapper;
import com.bretpatterson.schemagen.graphql.typemappers.IGraphQLTypeMapper;
import graphql.schema.GraphQLInputType;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLOutputType;

import java.lang.reflect.Array;
import java.lang.reflect.Type;

/**
 * Default Interface type mapper that convers all types of Array to
 * a GraphQLList of the array type.
 */
@GraphQLTypeMapper(type = Array.class)
public class ArrayMapper implements IGraphQLTypeMapper {
	@Override
    public boolean handlesType(IGraphQLObjectMapper graphQLObjectMapper, Type type) {
		Class<?> typeClass = graphQLObjectMapper.getClassFromType(type);
		return Array.class.isAssignableFrom(typeClass);
	}

	@Override
	public GraphQLOutputType getOutputType(IGraphQLObjectMapper graphQLObjectMapper, Type type) {
		Class<?> classType = (Class<?>) type;
		return new GraphQLList(graphQLObjectMapper.getOutputType(classType.getComponentType()));
	}

	@Override
	public GraphQLInputType getInputType(IGraphQLObjectMapper graphQLObjectMapper, Type type) {
		Class<?> classType = (Class<?>) type;
		return new GraphQLList(graphQLObjectMapper.getInputType(classType.getComponentType()));
	}

}
