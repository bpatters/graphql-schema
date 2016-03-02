package com.bretpatterson.schemagen.graphql.typemappers.java.net;

import com.bretpatterson.schemagen.graphql.IGraphQLObjectMapper;
import com.bretpatterson.schemagen.graphql.annotations.GraphQLTypeMapper;
import com.bretpatterson.schemagen.graphql.typemappers.IGraphQLTypeMapper;
import graphql.Scalars;
import graphql.schema.GraphQLInputType;
import graphql.schema.GraphQLOutputType;

import java.lang.reflect.Type;
import java.net.URI;


/**
 * Default URI mapper that converts a URI to a GraphQLString
 */
@GraphQLTypeMapper(type=URI.class)
public class URIMapper implements IGraphQLTypeMapper {

	@Override
    public boolean handlesType(IGraphQLObjectMapper graphQLObjectMapper, Type type) {
		return type == URI.class;
	}
	@Override
	public GraphQLOutputType getOutputType(IGraphQLObjectMapper objectMapper, Type type) {
		return Scalars.GraphQLString;
	}

	@Override
	public GraphQLInputType getInputType(IGraphQLObjectMapper objectMapper, Type type) {
		return Scalars.GraphQLString;
	}

}
