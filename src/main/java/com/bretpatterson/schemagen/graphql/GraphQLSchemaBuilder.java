package com.bretpatterson.schemagen.graphql;

import com.bretpatterson.schemagen.graphql.annotations.GraphQLController;
import com.bretpatterson.schemagen.graphql.annotations.GraphQLDescription;
import com.bretpatterson.schemagen.graphql.annotations.GraphQLTypeMapper;
import com.bretpatterson.schemagen.graphql.datafetchers.CollectionConverterDataFetcher;
import com.bretpatterson.schemagen.graphql.datafetchers.DefaultTypeConverter;
import com.bretpatterson.schemagen.graphql.datafetchers.IDataFetcher;
import com.bretpatterson.schemagen.graphql.datafetchers.MapConverterDataFetcher;
import com.bretpatterson.schemagen.graphql.impl.GraphQLObjectMapper;
import com.bretpatterson.schemagen.graphql.impl.SimpleTypeFactory;
import com.bretpatterson.schemagen.graphql.relay.IRelayNodeFactory;
import com.bretpatterson.schemagen.graphql.relay.annotations.RelayNodeFactory;
import com.bretpatterson.schemagen.graphql.relay.impl.RelayDefaultNodeHandler;
import com.bretpatterson.schemagen.graphql.typemappers.IGraphQLTypeMapper;
import com.bretpatterson.schemagen.graphql.utils.AnnotationUtils;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Optional;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.reflect.ClassPath;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Main interface to generating your GraphQLSchema. This builder object allows you to configure your environment.
 * <UL>
 *     <LI> Controller Objects - Objects that contain top level Queries and Mutations</LI>
 *     <LI> Type Mappers - Custom {@link IGraphQLTypeMapper} that know how to convert JavaTypes into GraphQLTypes</LI>
 *     <LI> Type Factory - Custom {@link ITypeFactory} that knows how to convert generic deserialized JSON into Java Types. Currently only used to convert Query/Mutation Parameters to Method signature parameter types.</LI>
 *     <LI> Type Naming Strategy - Optional - Register your own custom Name strategy for naming all JavaTypes to GraphQLTypes. Default TypeMappers use this, custom ones should honor it where possible. </LI>
 *     <LI> Node Factories - These are factories that the {@link RelayDefaultNodeHandler} uses to handle node(id:string) calls and find the requested object by ID.</LI>
 * </UL>
 *
 * All GraphQL Input types, aka parameters, will have the _Input string appended to their Type. This ensure the input type name does not collide with the output type name when an object is used
 * for both input and output.
 *
 */
public class GraphQLSchemaBuilder {

	private static final Logger LOGGER = LoggerFactory.getLogger(GraphQLSchemaBuilder.class);

	private GraphQLSchema schema;
	private GraphQLObjectType.Builder rootQueryBuilder;
	private GraphQLObjectType.Builder rootMutationBuilder;
	private GraphQLObjectMapper graphQLObjectMapper;
	private List<Object> graphQLControllers = new LinkedList<>();
	private List<IGraphQLTypeMapper> typeMappers = new LinkedList<>();
	private Optional<ITypeNamingStrategy> typeNamingStrategy = Optional.absent();
	private Optional<IDataFetcherFactory> dataFetcherFactory = Optional.absent();
	private Map<Class<?>, Class<? extends DefaultTypeConverter>> defaultTypeConverters;
	private Optional<Class<? extends IDataFetcher>> defaultMethodDataFetcher = Optional.absent();
	private RelayDefaultNodeHandler.Builder relayDefaultNodeHandler = RelayDefaultNodeHandler.builder();
	private List<Class<?>> relayNodeTypes = Lists.newArrayList();
	private ITypeFactory typeFactory = new SimpleTypeFactory();
	private ClassLoader classLoader;
	private ClassPath classPath;
	private boolean relayEnabled = false;

	public GraphQLSchemaBuilder() {
		this.rootQueryBuilder = GraphQLObjectType.newObject().name("Query").description("Root of Query Schema");
		this.rootMutationBuilder = GraphQLObjectType.newObject().name("Mutation").description("Root of Mutation Schema");
		this.defaultTypeConverters = getDefaultTypeConverters();
		try {
			classLoader = getClass().getClassLoader();
			classPath = ClassPath.from(classLoader);
		} catch (IOException ex) {
			Throwables.propagate(ex);
		}
	}

	public static GraphQLSchemaBuilder newBuilder() {
		return new GraphQLSchemaBuilder();
	}

	/**
	 * Register objects to be scanned as containing top level query fields
	 * 
	 * @param queryHandlers
	 * @return
	 */
	public GraphQLSchemaBuilder registerGraphQLControllerObjects(List<Object> queryHandlers) {
		this.graphQLControllers.addAll(queryHandlers);

		return this;
	}

	/**
	 * Register custom type mapper instances that can be used to convert types to GraphQLTypes. These type mappers will have precidence over
	 * any other types discovered.
	 * 
	 * @param typeMappers
	 * @return
	 */
	public GraphQLSchemaBuilder registerTypeMappers(List<IGraphQLTypeMapper> typeMappers) {
		this.typeMappers.addAll(typeMappers);

		return this;
	}


	/**
	 * This typeFactory will be used to convert between GraphQL value types to Java native value types when passing parameters to
	 * DataFetchers. This object will be assigned to all datafetchers created for GraphQL queries and mutations.
	 * 
	 * @param typeFactory
	 * @return
	 */
	public GraphQLSchemaBuilder registerTypeFactory(ITypeFactory typeFactory) {
		this.typeFactory = typeFactory;

		return this;
	}

	/**
	 * Register a new Type Naming strategy to be used in place of the default SimpleTypeNamingStrategy See {@link ITypeNamingStrategy}
	 * 
	 * @param strategy
	 * @return
	 */
	public GraphQLSchemaBuilder registerTypeNamingStrategy(ITypeNamingStrategy strategy) {
		typeNamingStrategy = Optional.fromNullable(strategy);

		return this;
	}

	/**
	 * Register your Relay Node Factory instances for handling mapping from node id's to objects.
	 * 
	 * @see <a href="https://facebook.github.io/relay/graphql/objectidentification.htm">Graph QL Relay Object Identification</a>
	 *
	 * @param nodeFactories the factories to register
	 * @return
	 */
	public GraphQLSchemaBuilder registerNodeFactories(List<IRelayNodeFactory> nodeFactories) {
		for (IRelayNodeFactory nodeFactory : nodeFactories) {
			RelayNodeFactory factoryAnnotation = nodeFactory.getClass().getAnnotation(RelayNodeFactory.class);
			relayDefaultNodeHandler.registerFactory(nodeFactory);
			relayNodeTypes.addAll(Lists.newArrayList(factoryAnnotation.types()));
		}

		return this;
	}

	public GraphQLSchemaBuilder registerDataFetcherFactory(IDataFetcherFactory dataFetcherFactory) {
		this.dataFetcherFactory = Optional.fromNullable(dataFetcherFactory);

		return this;
	}

	public GraphQLSchemaBuilder registerDefaultMethodDataFetcher(Class<? extends IDataFetcher> defaultMethodDataFetcher) {
		this.defaultMethodDataFetcher = Optional.<Class<? extends IDataFetcher>>fromNullable(defaultMethodDataFetcher);

		return this;
	}

	/**
	 * These are used for converting field types to other types after datafetching. For example if you need to convert
	 * a Long to a String you would register a type convert for type Long that knew how to convert Longs into strings.
	 * By default we incldue the following type converters.
	 * Collection {@literal --->} List
	 * Map        {@literal --->} List  (list of entries)
	 *
	 * These are essentially DataFetchers that delegate to another datafetcher and then convert the returned value.
	 * @param typeConverters mpa of the types to type converters.
	 * @return
	 */
	public GraphQLSchemaBuilder registerDefaultTypeConverters(Map<Class<?>, Class<? extends DefaultTypeConverter>> typeConverters) {
		this.defaultTypeConverters = typeConverters;

		return this;
	}

	public GraphQLSchemaBuilder relayEnabled(boolean relayEnabled) {
		this.relayEnabled = relayEnabled;

		return this;
	}

	@VisibleForTesting
	public static List<IGraphQLTypeMapper> getDefaultTypeMappers() {
		// install all of the default type mappers we include in our packages
		ImmutableList.Builder<IGraphQLTypeMapper> builder = ImmutableList.builder();
		try {
			Set<Class<?>> defaultTypeMappers = AnnotationUtils.getClassesWithAnnotation(GraphQLTypeMapper.class, IGraphQLTypeMapper.class.getPackage().getName())
					.keySet();
			for (Class<?> typeMapper : defaultTypeMappers) {
				builder.add((IGraphQLTypeMapper) typeMapper.newInstance());
			}

		} catch (InstantiationException | IllegalAccessException ex) {
			Throwables.propagate(ex);
		}
		return builder.build();
	}

	@VisibleForTesting
	public static Map<Class<?>, Class<? extends DefaultTypeConverter>> getDefaultTypeConverters() {
		return ImmutableMap.<Class<?>, Class<? extends DefaultTypeConverter>>of(Collection.class, CollectionConverterDataFetcher.class, Map.class, MapConverterDataFetcher.class);
	}

	public GraphQLSchema build() {

		this.typeMappers.addAll(0, getDefaultTypeMappers());
		this.setGraphQLObjectMapper(new GraphQLObjectMapper(typeFactory, typeMappers, typeNamingStrategy, dataFetcherFactory, defaultMethodDataFetcher, defaultTypeConverters, relayNodeTypes));
		// add our node handler first, as it's used by relay and we want people to be able to override it if they really want to
		if (relayEnabled) {
			graphQLControllers.add(0, relayDefaultNodeHandler.build());
		}

		ImmutableList.Builder<GraphQLFieldDefinition> rootViewFieldsBuilder  = ImmutableList.builder();
		ImmutableList.Builder<GraphQLFieldDefinition> rootMutationFieldsBuilder = ImmutableList.builder();


		for (Object queryHandler : graphQLControllers) {
			GraphQLController graphQLController = queryHandler.getClass().getAnnotation(GraphQLController.class);
			try {
				List<GraphQLFieldDefinition> viewFields = graphQLController.queryFactory().newInstance().newMethodQueriesForObject(getGraphQLObjectMapper(), queryHandler);
				List<GraphQLFieldDefinition> mutFields = graphQLController.mutationFactory().newInstance().newMethodMutationsForObject(getGraphQLObjectMapper(), queryHandler);

				if (viewFields.size() > 0) {
					if (AnnotationUtils.isNullValue(graphQLController.rootQueriesObjectName())) {
						rootViewFieldsBuilder.addAll(viewFields);
					}
					else {
						// creat root object field with the controllers root object name to hold the queries object wrapper
						GraphQLFieldDefinition.Builder rootViewField = GraphQLFieldDefinition.newFieldDefinition().name(graphQLController.rootQueriesObjectName()).staticValue(queryHandler);
						// create field object to contain this controllers query fields
						GraphQLObjectType.Builder viewerObject = GraphQLObjectType.newObject().name(graphQLController.rootQueriesObjectName());
						if (!AnnotationUtils.isNullValue(graphQLController.queryDescription())) {
							viewerObject.description(graphQLController.queryDescription());
						}
						viewerObject.fields(viewFields);

						rootViewField.type(viewerObject.build());
						rootViewFieldsBuilder.add(rootViewField.build());
					}
				}

				if (mutFields.size() > 0) {
					if (AnnotationUtils.isNullValue(graphQLController.rootMutationsObjectName())) {
						rootMutationFieldsBuilder.addAll(mutFields);
					}
					else {
						// create root object field with the controllers root object name to hold the mutations object wrapper
						GraphQLFieldDefinition.Builder rootMutationField = GraphQLFieldDefinition.newFieldDefinition().name(graphQLController.rootMutationsObjectName()).staticValue(queryHandler);
						// create field object to contain this controllers mutation fields
						GraphQLObjectType.Builder mutObject = GraphQLObjectType.newObject().name(graphQLController.rootMutationsObjectName());
						if (!AnnotationUtils.isNullValue(graphQLController.mutationDescription())) {
							mutObject.description(graphQLController.mutationDescription());
						}
						mutObject.fields(mutFields);

						rootMutationField.type(mutObject.build());
						rootMutationFieldsBuilder.add(rootMutationField.build());
					}
				}

				// now do mutations
			} catch (InstantiationException | IllegalAccessException ex) {
				LOGGER.warn("Failed to load {}.  This is probably because of an unsatisfied runtime dependency.", ex);
			}
		}
		GraphQLSchema.Builder builder = GraphQLSchema.newSchema();

		List<GraphQLFieldDefinition> rootViewFields = rootViewFieldsBuilder.build();
		if (rootViewFields.size() > 0) {
			rootQueryBuilder.fields(rootViewFieldsBuilder.build());
			builder.query(rootQueryBuilder.build());
		}

		List<GraphQLFieldDefinition> rootMutationFields = rootMutationFieldsBuilder.build();
		if (rootMutationFields.size() > 0) {
			rootMutationBuilder.fields(rootMutationFieldsBuilder.build());
			builder.mutation(rootMutationBuilder.build());
		}
		schema = builder.build(graphQLObjectMapper.getInputTypes());

		return schema;
	}

	@VisibleForTesting
	GraphQLObjectMapper getGraphQLObjectMapper() {
		return graphQLObjectMapper;
	}

	@VisibleForTesting
	void setGraphQLObjectMapper(GraphQLObjectMapper graphQLObjectMapper) {
		this.graphQLObjectMapper = graphQLObjectMapper;
	}
}
