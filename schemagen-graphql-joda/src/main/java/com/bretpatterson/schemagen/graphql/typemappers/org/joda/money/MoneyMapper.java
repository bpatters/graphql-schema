package com.bretpatterson.schemagen.graphql.typemappers.org.joda.money;

import com.bretpatterson.schemagen.graphql.IGraphQLObjectMapper;
import com.bretpatterson.schemagen.graphql.annotations.GraphQLTypeMapper;
import com.bretpatterson.schemagen.graphql.typemappers.IGraphQLTypeMapper;
import graphql.Scalars;
import graphql.schema.GraphQLInputType;
import graphql.schema.GraphQLOutputType;
import org.joda.money.Money;

import java.lang.reflect.Type;


/**
 * Created by bpatterson on 1/19/16.
 */
@GraphQLTypeMapper(type=Money.class)
public class MoneyMapper implements IGraphQLTypeMapper {
	@Override
    public boolean handlesType(IGraphQLObjectMapper graphQLObjectMapper, Type type) {
		return type == Money.class;
	}

	@Override
	public GraphQLOutputType getOutputType(IGraphQLObjectMapper objectMapper, Type type) {
		return Scalars.GraphQLFloat;
	}

	@Override
	public GraphQLInputType getInputType(IGraphQLObjectMapper objectMapper,  Type type) {
		return Scalars.GraphQLFloat;
	}

}
