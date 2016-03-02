package com.bretpatterson.schemagen.graphql.typemappers.java.util;

import com.bretpatterson.schemagen.graphql.IGraphQLObjectMapper;
import com.bretpatterson.schemagen.graphql.annotations.GraphQLTypeMapper;
import com.bretpatterson.schemagen.graphql.exceptions.NotMappableException;
import com.bretpatterson.schemagen.graphql.typemappers.IGraphQLTypeMapper;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLInputObjectField;
import graphql.schema.GraphQLInputObjectType;
import graphql.schema.GraphQLInputType;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLOutputType;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Map;

/**
 * GraphQL doesn't support generic maps fully. However this implementation attempts to support them as best it can. It currently supports
 * {@code Map<Enum, Object> } since the set of keys is well defined. In this case it maps this datatype to an Object of Enum --> GraphQLType
 * where the keys of Enum are fields and values are the field values.
 */
@GraphQLTypeMapper(type = Map.class)
public class MapMapper implements IGraphQLTypeMapper {

	public static final String KEY_NAME = "key";
	public static final String VALUE_NAME = "value";

	@Override
    public boolean handlesType(IGraphQLObjectMapper graphQLObjectMapper, Type type) {
		Class<?> typeClass = graphQLObjectMapper.getClassFromType(type);
		return Map.class.isAssignableFrom(typeClass);
	}

	@Override
	public GraphQLOutputType getOutputType(IGraphQLObjectMapper graphQLObjectMapper, Type type) {
		if (type instanceof ParameterizedType) {
			return getListOutputMapping(graphQLObjectMapper, type);
		} else {
			throw new NotMappableException(String.format("%s is not mappable to GraphQL", graphQLObjectMapper.getTypeNamingStrategy().getTypeName(graphQLObjectMapper, type)));
		}
	}

	private GraphQLOutputType getListOutputMapping(final IGraphQLObjectMapper graphQLObjectMapper, final Type type) {
		ParameterizedType pType = (ParameterizedType) type;
		GraphQLObjectType objectType = GraphQLObjectType.newObject()
				.name(graphQLObjectMapper.getTypeNamingStrategy().getTypeName(graphQLObjectMapper, type))
				.field(GraphQLFieldDefinition.newFieldDefinition()
						.name(KEY_NAME)
						.type(graphQLObjectMapper.getOutputType(pType.getActualTypeArguments()[0]))
						.build())
				.field(GraphQLFieldDefinition.newFieldDefinition()
						.name(VALUE_NAME)
						.type(graphQLObjectMapper.getOutputType(pType.getActualTypeArguments()[1]))
						.build())
				.build();

		return new GraphQLList(objectType);
	}

	private GraphQLInputType getListInputMapping(final IGraphQLObjectMapper graphQLObjectMapper, final Type type) {
		ParameterizedType pType = (ParameterizedType) type;
		GraphQLInputObjectType objectType = GraphQLInputObjectType.newInputObject()
				.name(graphQLObjectMapper.getTypeNamingStrategy().getTypeName(graphQLObjectMapper, type))
				.field(GraphQLInputObjectField.newInputObjectField()
						.name(KEY_NAME)
						.type(graphQLObjectMapper.getInputType(pType.getActualTypeArguments()[0]))
						.build())
				.field(GraphQLInputObjectField.newInputObjectField()
						.name(VALUE_NAME)
						.type(graphQLObjectMapper.getInputType(pType.getActualTypeArguments()[1]))
						.build())
				.build();

		return new GraphQLList(objectType);
	}

	@Override
	public GraphQLInputType getInputType(IGraphQLObjectMapper graphQLObjectMapper, Type type) {
		if (type instanceof ParameterizedType) {
			return getListInputMapping(graphQLObjectMapper, type);
		} else {
			throw new NotMappableException(String.format("%s is not mappable to GraphQL", graphQLObjectMapper.getTypeNamingStrategy().getTypeName(graphQLObjectMapper, type)));
		}
	}

}
