package com.bretpatterson.schemagen.graphql.typemappers.java.util;

import com.bretpatterson.schemagen.graphql.IGraphQLObjectMapper;
import com.bretpatterson.schemagen.graphql.annotations.GraphQLTypeMapper;
import com.bretpatterson.schemagen.graphql.typemappers.IGraphQLTypeMapper;
import graphql.schema.GraphQLInputType;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLOutputType;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;

/**
 * Default interface mapper for all Collections. Converts all collections
 * into a GraphQLList type containing the type of object the collection contains.
 */
@GraphQLTypeMapper(type = Collection.class)
public class CollectionMapper implements IGraphQLTypeMapper {
	@Override
    public boolean handlesType(IGraphQLObjectMapper graphQLObjectMapper, Type type) {
		Class<?> typeClass = graphQLObjectMapper.getClassFromType(type);
		return Collection.class.isAssignableFrom(typeClass);
	}

	@Override
	public GraphQLOutputType getOutputType(IGraphQLObjectMapper graphQLObjectMapper, Type type) {
		if (type instanceof ParameterizedType) {
			ParameterizedType parameterizedType = (ParameterizedType) type;
			return new GraphQLList(graphQLObjectMapper.getOutputType(parameterizedType.getActualTypeArguments()[0]));
		} else {
			return new GraphQLList(graphQLObjectMapper.getOutputType(Object.class));
		}
	}

	@Override
	public GraphQLInputType getInputType(IGraphQLObjectMapper graphQLObjectMapper, Type type) {
		if (type instanceof ParameterizedType) {
			ParameterizedType parameterizedType = (ParameterizedType) type;
			return new GraphQLList(graphQLObjectMapper.getOutputType(parameterizedType.getActualTypeArguments()[0]));
		} else {
			return new GraphQLList(graphQLObjectMapper.getOutputType(Object.class));
		}
	}

}
