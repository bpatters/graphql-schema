package com.bretpatterson.schemagen.graphql.datafetchers;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import java.lang.reflect.Type;
import java.util.Collection;

/**
 * This converts any Collection object to a ImmutableList of the objects. Additionally this converts a null value to an empty list.
 */
public class CollectionConverterDataFetcher implements IDataFetcher {

	DataFetcher parentDataFetcher;

	public CollectionConverterDataFetcher(DataFetcher parentDataFetcher) {
		this.parentDataFetcher = parentDataFetcher;
	}

	@Override
	public Object get(DataFetchingEnvironment environment) {
		Collection<?> rv = (Collection<?>) parentDataFetcher.get(environment);

		if (rv == null) {
			return ImmutableList.of();
		}

		return ImmutableList.copyOf(rv);
	}

	@Override
	public void addParam(final String name, final Type type, final Optional<Object> defaultValue) {
		if (IDataFetcher.class.isAssignableFrom(parentDataFetcher.getClass())) {
			((IDataFetcher) parentDataFetcher).addParam(name, type, defaultValue);
		}
	}
}
