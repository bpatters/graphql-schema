package com.bretpatterson.schemagen.graphql.typemappers.java.util;

import com.bretpatterson.schemagen.graphql.IGraphQLObjectMapper;
import com.bretpatterson.schemagen.graphql.annotations.GraphQLTypeMapper;
import com.bretpatterson.schemagen.graphql.typemappers.IGraphQLTypeMapper;
import graphql.schema.GraphQLEnumType;
import graphql.schema.GraphQLInputType;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLOutputType;

import java.lang.reflect.Type;
import java.util.EnumSet;

/**
 * Default interface type mapper that handles all types of EnumSet.
 */
@GraphQLTypeMapper(type = EnumSet.class)
public class EnumSetMapper implements IGraphQLTypeMapper {
	@Override
    public boolean handlesType(IGraphQLObjectMapper graphQLObjectMapper, Type type) {
		Class<?> typeClass = graphQLObjectMapper.getClassFromType(type);
		return EnumSet.class.isAssignableFrom(typeClass);
	}

	@Override
	public GraphQLOutputType getOutputType(IGraphQLObjectMapper graphQLObjectMapper, Type type) {
		Class<?> classType = (Class<?>) type;
		Class<?> enumClassType = classType.getComponentType();
		GraphQLEnumType.Builder enumType = GraphQLEnumType.newEnum()
				.name(graphQLObjectMapper.getTypeNamingStrategy().getTypeName(graphQLObjectMapper, enumClassType));

		for (Object value : enumClassType.getEnumConstants()) {
			enumType.value(value.toString(), value);
		}
		return new GraphQLList(enumType.build());

	}

	@Override
	public GraphQLInputType getInputType(IGraphQLObjectMapper graphQLObjectMapper, Type type) {
		Class<?> classType = (Class<?>) type;
		Class<?> enumClassType = classType.getComponentType();
		GraphQLEnumType.Builder enumType = GraphQLEnumType.newEnum()
				.name(graphQLObjectMapper.getTypeNamingStrategy().getTypeName(graphQLObjectMapper, enumClassType));

		for (Object value : enumClassType.getEnumConstants()) {
			enumType.value(value.toString(), value);
		}
		return new GraphQLList(enumType.build());
	}

}
