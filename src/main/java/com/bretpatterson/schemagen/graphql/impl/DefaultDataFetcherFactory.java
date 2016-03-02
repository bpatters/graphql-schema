package com.bretpatterson.schemagen.graphql.impl;

import com.bretpatterson.schemagen.graphql.annotations.GraphQLParam;
import com.bretpatterson.schemagen.graphql.datafetchers.IDataFetcher;
import com.bretpatterson.schemagen.graphql.IDataFetcherFactory;
import com.bretpatterson.schemagen.graphql.IGraphQLObjectMapper;
import com.bretpatterson.schemagen.graphql.datafetchers.IMethodDataFetcher;
import com.bretpatterson.schemagen.graphql.utils.AnnotationUtils;
import com.google.common.base.Optional;
import com.google.common.base.Throwables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.security.InvalidParameterException;

/**
 * The Default DataFetcher Factory. This only supports custom datafetchers of type IMethodDataFetcher
 */
public class DefaultDataFetcherFactory implements IDataFetcherFactory {

	private static final Logger LOGGER = LoggerFactory.getLogger(DefaultDataFetcherFactory.class);

	@Override
	public IDataFetcher newFieldDataFetcher(final IGraphQLObjectMapper objectMapper, final Field field, Class<? extends IDataFetcher> dataFetcher) {
		if (dataFetcher != null) {
			try {
				return dataFetcher.newInstance();
			} catch (IllegalAccessException | InstantiationException ex) {
				throw Throwables.propagate(ex);
			}
		}

		return null;
	}

	@Override
	public IDataFetcher newMethodDataFetcher(final IGraphQLObjectMapper graphQLObjectMapper,
			final Object targetObject,
			final Method method,
			final String fieldName,
			final Class<? extends IDataFetcher> dataFetcher) {
		IDataFetcher dataFetcherObject;
		try {
			dataFetcherObject = (IDataFetcher) dataFetcher.newInstance();
			if (!IMethodDataFetcher.class.isAssignableFrom(dataFetcher)) {
				throw new InvalidParameterException("This Data Fetcher Factory only supports IMethodDataFetchers");
			}
			IMethodDataFetcher methodDataFetcher = (IMethodDataFetcher) dataFetcherObject;
			methodDataFetcher.setFieldName(fieldName);
			methodDataFetcher.setTypeFactory(graphQLObjectMapper.getTypeFactory());
			methodDataFetcher.setTargetObject(targetObject);
			methodDataFetcher.setMethod(method);
			processMethodArguments(methodDataFetcher, method);
		} catch (Exception ex) {
			throw Throwables.propagate(ex);
		}
		return dataFetcherObject;
	}

	protected void processMethodArguments(IDataFetcher dataFetcher, Method method) {
		Annotation[][] parameterAnnotations = method.getParameterAnnotations();
		int index = 0;
		for (Type paramType : method.getGenericParameterTypes()) {
			GraphQLParam graphQLParam = AnnotationUtils.findAnnotation(parameterAnnotations[index], GraphQLParam.class);

			if (graphQLParam == null) {
				LOGGER.error("Missing @GraphParam annotation on parameter index {} for method {}", index, method.getName());
				continue;
			}

			dataFetcher.addParam(graphQLParam.name(),
					paramType,
					Optional.<Object> fromNullable(AnnotationUtils.isNullValue(graphQLParam.defaultValue()) ? null : graphQLParam.defaultValue()));

			index++;
		}
	}
}
