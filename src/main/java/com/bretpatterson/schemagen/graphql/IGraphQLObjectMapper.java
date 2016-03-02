package com.bretpatterson.schemagen.graphql;


import com.bretpatterson.schemagen.graphql.datafetchers.IDataFetcher;
import graphql.schema.GraphQLInputType;
import graphql.schema.GraphQLOutputType;
import graphql.schema.GraphQLType;

import java.lang.reflect.Type;
import java.util.Set;

/**
 * A GraphQLObjectMapper knows how to build a GraphQLDefinition for objects.
 */
public interface IGraphQLObjectMapper {

	/**
	 * Get the object responsible for naming GraphQL types.
	 * @return
	 */
	ITypeNamingStrategy getTypeNamingStrategy();

	/**
	 * Get an input definition for the specified type.
	 * @param type
	 */
	GraphQLInputType getInputType(Type type);

	/**
	 * Get an output definition for the specified type.
	 * @param type
	 */
	GraphQLOutputType getOutputType(Type type);

	/**
	 * Get the Input Type cache. This is useful for custom type mappers who
	 * might need to add TypeReference's to the cache when they need to process
	 * an object that contains a two way dependency between itself and another object.
	 */
	IGraphQLTypeCache<GraphQLInputType> getInputTypeCache();

	/**
	 * Get the Input Type cache. This is useful for custom type mappers who
	 * might need to add TypeReference's to the cache when they need to process
	 * an object that contains a two way dependency between itself and another object.
	 */
	IGraphQLTypeCache<GraphQLOutputType> getOutputTypeCache();

	/**
	 * Returns the object responsible for object type conversion.
	 * @return
	 */
	ITypeFactory getTypeFactory();

	/**
	 * Get the raw type of this object for generic types
	 * @param type
	 * @return
	 */
	Class<?> getClassFromType(Type type);

	/**
	 * Returns all input types created.
	 * @return
	 */
	Set<GraphQLType> getInputTypes();

	/**
	 * Get the datafetcher factory
	 * @return
	 */
	IDataFetcherFactory getDataFetcherFactory() ;

	/**
	 *
	 * @param dataFetcherFactory
	 */
	void setDataFetcherFactory(IDataFetcherFactory dataFetcherFactory);

	/**
	 * Get the default datafetcher for methods
	 * @return
	 */
	Class<? extends IDataFetcher> getDefaultMethodDataFetcher();

	/**
	 * Set the default data fetcher used for methods
	 * @param defaultMethodDataFetcher
	 */
	void setDefaultMethodDataFetcher(Class<? extends IDataFetcher> defaultMethodDataFetcher);
}
