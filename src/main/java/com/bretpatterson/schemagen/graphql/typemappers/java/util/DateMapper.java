package com.bretpatterson.schemagen.graphql.typemappers.java.util;

import com.bretpatterson.schemagen.graphql.IGraphQLObjectMapper;
import com.bretpatterson.schemagen.graphql.annotations.GraphQLTypeMapper;
import com.bretpatterson.schemagen.graphql.typemappers.IGraphQLTypeMapper;
import graphql.Scalars;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLInputObjectField;
import graphql.schema.GraphQLInputObjectType;
import graphql.schema.GraphQLInputType;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLOutputType;

import java.lang.reflect.Type;
import java.util.Date;

/**
 * Default Date Mapper that converts a Date to a GraphQL Long
 */
@GraphQLTypeMapper(type= Date.class)
public class DateMapper implements IGraphQLTypeMapper {
	@Override
    public boolean handlesType(IGraphQLObjectMapper graphQLObjectMapper, Type type) {
		return type == Date.class;
	}

	@Override
	public GraphQLOutputType getOutputType(IGraphQLObjectMapper objectMapper, Type type) {
		return GraphQLObjectType.newObject().name(objectMapper.getTypeNamingStrategy().getTypeName(objectMapper, type))
				.field(GraphQLFieldDefinition.newFieldDefinition().name("time").type(Scalars.GraphQLString).build()).build();

	}

	@Override
	public GraphQLInputType getInputType(IGraphQLObjectMapper objectMapper,  Type type) {
		return GraphQLInputObjectType.newInputObject().name(objectMapper.getTypeNamingStrategy().getTypeName(objectMapper, type))
									 .field(GraphQLInputObjectField.newInputObjectField().name("time").type(Scalars.GraphQLString).build()).build();
	}

}
