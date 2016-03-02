package com.bretpatterson.schemagen.graphql.datafetchers;

import com.google.common.base.Optional;
import graphql.schema.DataFetcher;

import java.lang.reflect.Type;

/**
 * Created by bpatterson on 2/19/16.
 */
public interface IDataFetcher extends DataFetcher {
	/**
	 * When generating a GraphQL Field definition we make multiple call
	 * to this method to add it's parameters. The invocation order will be the exact order
	 * of the parameters.
	 * @param name the parameter name
	 * @param type the generic type of the parameter
	 * @param defaultValue optionally the default value defined
	 */
	void addParam(String name, Type type, Optional<Object> defaultValue);
}
