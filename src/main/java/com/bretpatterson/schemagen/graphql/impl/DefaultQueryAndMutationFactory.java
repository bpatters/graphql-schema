package com.bretpatterson.schemagen.graphql.impl;

import com.bretpatterson.schemagen.graphql.datafetchers.IDataFetcher;
import com.bretpatterson.schemagen.graphql.IGraphQLObjectMapper;
import com.bretpatterson.schemagen.graphql.IMutationFactory;
import com.bretpatterson.schemagen.graphql.IQueryFactory;
import com.bretpatterson.schemagen.graphql.annotations.GraphQLMutation;
import com.bretpatterson.schemagen.graphql.annotations.GraphQLParam;
import com.bretpatterson.schemagen.graphql.annotations.GraphQLQuery;
import com.bretpatterson.schemagen.graphql.utils.AnnotationUtils;
import com.google.common.base.Optional;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import graphql.schema.GraphQLArgument;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLOutputType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

/**
 * Default implementation of the query/mutation factory. Converts a Method signature on an object into a:
 * <OL>
 * <LI>List of GraphQL field definitions</LI>
 * <LI>GraphQLOutput type for the return type of the method.</LI>
 * <LI>Configures the Datafetcher based on the Object Instance, Field Name, Method Signature, and Type Factory</LI>
 * </OL>
 */
public class DefaultQueryAndMutationFactory implements IQueryFactory, IMutationFactory {

	private static final Logger LOGGER = LoggerFactory.getLogger(DefaultQueryAndMutationFactory.class);

	@Override
    public List<GraphQLFieldDefinition> newMethodMutationsForObject(IGraphQLObjectMapper graphQLObjectMapper,
			Object targetObject) {
		ImmutableList.Builder<GraphQLFieldDefinition> results = ImmutableList.builder();
		Map<GraphQLMutation, Method> methodMap = AnnotationUtils.getMethodsWithAnnotation(targetObject.getClass(), GraphQLMutation.class);

		for (Map.Entry<GraphQLMutation, Method> entry : methodMap.entrySet()) {
			try {
				Class<? extends IDataFetcher> dataFetcher = entry.getKey().dataFetcher();
				if (AnnotationUtils.isNullValue(dataFetcher)) {
					dataFetcher = graphQLObjectMapper.getDefaultMethodDataFetcher();
				}
				GraphQLFieldDefinition newField = buildFieldDefinition(graphQLObjectMapper,
						targetObject,
						entry.getKey().name(),
						entry.getValue(),
						dataFetcher);
				results.add(newField);
			} catch (Exception ex) {
				Throwables.propagate(ex);
			}
		}

		return results.build();
	}

	private GraphQLFieldDefinition buildFieldDefinition(IGraphQLObjectMapper graphQLObjectMapper,
			Object targetObject,
			String name,
			Method method,
			Class<? extends IDataFetcher> dataFetcherClass) {
		IDataFetcher dataFetcher = graphQLObjectMapper.getDataFetcherFactory().newMethodDataFetcher(graphQLObjectMapper,
																									targetObject,
																									method,
																									name,
																									dataFetcherClass);

		GraphQLFieldDefinition.Builder newField = GraphQLFieldDefinition.newFieldDefinition().name(name);
		newField.type(getReturnType(graphQLObjectMapper, method));
		if (dataFetcher !=null) {
			newField.dataFetcher(dataFetcher);
		}
		List<GraphQLArgument> arguments = getMethodArguments(graphQLObjectMapper, Optional.fromNullable(dataFetcher), method);

		newField.argument(arguments);

		return newField.build();
	}

	/**
	 * Generates field definitions, with arguments, from all {@link GraphQLQuery} annotated methods on the source object.
	 * 
	 * @param graphQLObjectMapper the current IGraphQLObjectMapper
	 * @param targetObject the target object the DataFetcher will invoke the method on.
	 * @return
	 */
	@Override
    public List<GraphQLFieldDefinition> newMethodQueriesForObject(IGraphQLObjectMapper graphQLObjectMapper, Object targetObject) {
		ImmutableList.Builder<GraphQLFieldDefinition> results = ImmutableList.builder();
		Map<GraphQLQuery, Method> methodMap = AnnotationUtils.getMethodsWithAnnotation(targetObject.getClass(), GraphQLQuery.class);

		for (Map.Entry<GraphQLQuery, Method> entry : methodMap.entrySet()) {
			try {
				Class<? extends IDataFetcher> dataFetcher = entry.getKey().dataFetcher();
				if (AnnotationUtils.isNullValue(dataFetcher)) {
					dataFetcher = graphQLObjectMapper.getDefaultMethodDataFetcher();
				}
				GraphQLFieldDefinition newField = buildFieldDefinition(graphQLObjectMapper,
						targetObject,
						entry.getKey().name(),
						entry.getValue(),
						dataFetcher);
				results.add(newField);
			} catch (Exception ex) {
				throw Throwables.propagate(ex);
			}
		}

		return results.build();
	}

	protected GraphQLOutputType getReturnType(IGraphQLObjectMapper graphQLObjectMapper, Method method) {
		return graphQLObjectMapper.getOutputType(method.getGenericReturnType());
	}

	/**
	 * Generates a list of GraphQLArgument objects for the specified method while passing parameter information to the datafetcher for use
	 * during fetching.
	 * 
	 * @param graphQLObjectMapper the graphQLObject mapper
	 * @param dataFetcher The Data fetcher that will be used for this method.
	 * @param method
	 * @return
	 * @throws Exception
	 */
	protected List<GraphQLArgument> getMethodArguments(IGraphQLObjectMapper graphQLObjectMapper, Optional<IDataFetcher> dataFetcher, Method method) {
		ImmutableList.Builder<GraphQLArgument> argumentBuilder = ImmutableList.builder();
		GraphQLArgument.Builder paramBuilder;
		Annotation[][] parameterAnnotations = method.getParameterAnnotations();
		int index = 0;
		for (Type paramType : method.getGenericParameterTypes()) {
			GraphQLParam graphQLParam = AnnotationUtils.findAnnotation(parameterAnnotations[index], GraphQLParam.class);

			if (graphQLParam == null) {
				LOGGER.error("Missing @GraphParam annotation on parameter index {} for method {}", index, method.getName());
				continue;
			}

			paramBuilder = GraphQLArgument.newArgument().name(graphQLParam.name()).type(graphQLObjectMapper.getInputType(paramType));
			if (dataFetcher.isPresent()) {
				dataFetcher.get().addParam(graphQLParam.name(),
						paramType,
						Optional.<Object> fromNullable(AnnotationUtils.isNullValue(graphQLParam.defaultValue()) ? null : graphQLParam.defaultValue()));
			}

			argumentBuilder.add(paramBuilder.build());
			index++;
		}

		return argumentBuilder.build();
	}

}
