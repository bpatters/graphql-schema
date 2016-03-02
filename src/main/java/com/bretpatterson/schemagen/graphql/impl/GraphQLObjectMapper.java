package com.bretpatterson.schemagen.graphql.impl;

import com.bretpatterson.schemagen.graphql.IDataFetcherFactory;
import com.bretpatterson.schemagen.graphql.IGraphQLObjectMapper;
import com.bretpatterson.schemagen.graphql.IGraphQLTypeCache;
import com.bretpatterson.schemagen.graphql.IQueryFactory;
import com.bretpatterson.schemagen.graphql.ITypeNamingStrategy;
import com.bretpatterson.schemagen.graphql.annotations.GraphQLController;
import com.bretpatterson.schemagen.graphql.annotations.GraphQLDataFetcher;
import com.bretpatterson.schemagen.graphql.annotations.GraphQLIgnore;
import com.bretpatterson.schemagen.graphql.annotations.GraphQLTypeMapper;
import com.bretpatterson.schemagen.graphql.datafetchers.CollectionConverterDataFetcher;
import com.bretpatterson.schemagen.graphql.ITypeFactory;
import com.bretpatterson.schemagen.graphql.datafetchers.DefaultMethodDataFetcher;
import com.bretpatterson.schemagen.graphql.datafetchers.IDataFetcher;
import com.bretpatterson.schemagen.graphql.datafetchers.MapConverterDataFetcher;
import com.bretpatterson.schemagen.graphql.exceptions.NotMappableException;
import com.bretpatterson.schemagen.graphql.relay.INode;
import com.bretpatterson.schemagen.graphql.typemappers.IGraphQLTypeMapper;
import com.bretpatterson.schemagen.graphql.utils.AnnotationUtils;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Optional;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import graphql.Scalars;
import graphql.schema.DataFetcher;
import graphql.schema.GraphQLEnumType;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLInputObjectField;
import graphql.schema.GraphQLInputObjectType;
import graphql.schema.GraphQLInputType;
import graphql.schema.GraphQLInterfaceType;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLOutputType;
import graphql.schema.GraphQLScalarType;
import graphql.schema.GraphQLType;
import graphql.schema.GraphQLTypeReference;
import graphql.schema.PropertyDataFetcher;
import graphql.schema.TypeResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

/**
 * This is the meat of the schema gen package. Utilizing the configured properties it will traverse the objects provided and generate a type
 * hierarchy for GraphQL.
 */
public class GraphQLObjectMapper implements IGraphQLObjectMapper, TypeResolver {

	private static final Logger LOGGER = LoggerFactory.getLogger(GraphQLObjectMapper.class);
	// class type mappers
	private ImmutableMap<Type, IGraphQLTypeMapper> classTypeMappers;
	private ImmutableList<IGraphQLTypeMapper> interfaceTypeMappers;
	private IGraphQLTypeCache<GraphQLOutputType> outputTypeCache = new DefaultGraphQLTypeCache<>();
	private IGraphQLTypeCache<GraphQLInputType> inputTypeCache = new DefaultGraphQLTypeCache<>();
	private ITypeFactory typeFactory;
	private ITypeNamingStrategy typeNamingStrategy = new SimpleTypeNamingStrategy();
	private List<Class<?>> relayNodeTypes;
	private Stack<Map<String, Type>> typeArguments = new Stack<>();
	private Set<GraphQLType> inputTypes = Sets.newHashSet();
	private String nodeTypeName;
	private IDataFetcherFactory dataFetcherFactory = new DefaultDataFetcherFactory();
	private Class<? extends IDataFetcher> defaultMethodDataFetcher;

	public GraphQLObjectMapper(ITypeFactory typeFactory, List<IGraphQLTypeMapper> graphQLTypeMappers, Optional<ITypeNamingStrategy> typeNamingStrategy,
			Optional<IDataFetcherFactory> dataFetcherFactory, Optional<Class<? extends IDataFetcher>> defaultMethodDataFetcher, List<Class<?>> relayNodeTypes) {

		this.typeFactory = typeFactory;
		this.relayNodeTypes = relayNodeTypes;
		this.setDefaultMethodDataFetcher(defaultMethodDataFetcher.or(DefaultMethodDataFetcher.class));

		if (typeNamingStrategy.isPresent()) {
			this.typeNamingStrategy = typeNamingStrategy.get();
		}

		if (dataFetcherFactory.isPresent()) {
			this.setDataFetcherFactory(dataFetcherFactory.get());
		}

		ImmutableList.Builder<IGraphQLTypeMapper> interfaceTypeMappersBuilder = ImmutableList.builder();
		Map<Type, IGraphQLTypeMapper> classTypeMappersBuilder = Maps.newHashMap();

		for (IGraphQLTypeMapper mapper : graphQLTypeMappers) {
			GraphQLTypeMapper mapperAnnotation = mapper.getClass().getAnnotation(GraphQLTypeMapper.class);
			if (mapperAnnotation.type().isInterface()) {
				interfaceTypeMappersBuilder.add(mapper);
			} else {
				classTypeMappersBuilder.put(mapperAnnotation.type(), mapper);
			}
		}
		this.interfaceTypeMappers = interfaceTypeMappersBuilder.build();
		this.classTypeMappers = ImmutableMap.copyOf(classTypeMappersBuilder);

		// store the Node type interface mapping
		nodeTypeName = getTypeNamingStrategy().getTypeName(this, INode.class);
		this.getOutputTypeCache().put(nodeTypeName,
				GraphQLInterfaceType.newInterface()
						.name(nodeTypeName)
						.typeResolver(this)
						.field(GraphQLFieldDefinition.newFieldDefinition().name("id").type(Scalars.GraphQLString).build())
						.build());
	}

	private void buildGenericArgumentTypeMap(ParameterizedType type) {
		Class<?> rawClass = (Class<?>) type.getRawType();
		TypeVariable<?>[] typeVariables = rawClass.getTypeParameters();
		Type[] arguments = type.getActualTypeArguments();
		for (int i = 0; i < typeVariables.length; i++) {
			// field definitions can cause us to come in here so we ignore type variable argument types.
			if (!(arguments[i] instanceof TypeVariable)) {
				typeArguments.peek().put(typeVariables[i].getName(), arguments[i]);
			} else {
				// we might be mapping one variable name to another so do that here
				// IE: MyObject<R,S> {
				// MyInnerObject<S,R>
				// }
				// MyInnerOBject<R,S> {
				// R rType;
				// }
				// we need to update the current with the type from the parents map, so we pop, update, push
				Map<String, Type> current = typeArguments.pop();
				current.put(typeVariables[i].getName(), typeArguments.peek().get(((TypeVariable<?>) arguments[i]).getName()));
				typeArguments.push(current);
			}
		}
	}

	private Optional<String> getFieldNameFromMethod(Method m) {
		if (m.getName().startsWith("get")) {
			return Optional.of(m.getName().substring(3, 4).toLowerCase() + m.getName().substring(4));
		}
		if (m.getName().startsWith("is")) {
			return Optional.of(m.getName().substring(2, 3).toLowerCase() + m.getName().substring(3));
		}
		return Optional.absent();
	}

	private Optional<GraphQLFieldDefinition> getFieldType(Type type, Method method) {
		Type fieldType = method.getGenericReturnType();
		Class<?> fieldTypeClass = getClassFromType(fieldType);
		Optional<String> fieldName = getFieldNameFromMethod(method);
		GraphQLOutputType graphQLFieldType = getOutputType(fieldType);
		Class<? extends IDataFetcher> dataFetcherClass = getDefaultMethodDataFetcher();

		if (!fieldName.isPresent()) {
			return Optional.absent();
		}

		GraphQLFieldDefinition.Builder builder = GraphQLFieldDefinition.newFieldDefinition().name(fieldName.get()).type(graphQLFieldType);

		// first check if it has a custom type mapper, if so attach the dataFetcher to the field if specified
		Optional<IGraphQLTypeMapper> typeMapper = getCustomTypeMapper(fieldType);
		if (typeMapper.isPresent()) {
			GraphQLTypeMapper typeMapperAnnotation = typeMapper.get().getClass().getAnnotation(GraphQLTypeMapper.class);
			if (!AnnotationUtils.isNullValue(typeMapperAnnotation.dataFetcher())) {
				dataFetcherClass = typeMapperAnnotation.dataFetcher();
			}
		}

		// next check if there is annotation on the method
		GraphQLDataFetcher dataFetcherAnnotation = method.getAnnotation(GraphQLDataFetcher.class);
		// next check field for a datafetcher annotation in case it's not on the method
		if (dataFetcherAnnotation == null) {
			Class<?> typeClass = getClassFromType(type);
			try {
				Field field = typeClass.getDeclaredField(fieldName.get());
				if (field != null) {
					dataFetcherAnnotation = field.getAnnotation(GraphQLDataFetcher.class);

				}
			} catch (Exception ex) {
				LOGGER.info("No field matching method:" + method.getName());
			}
		}

		// explicit annotation for data fetcher?
		if (dataFetcherAnnotation != null) {
			dataFetcherClass = dataFetcherAnnotation.dataFetcher();
		}

		IDataFetcher dataFetcher;
		// if we have a datafetcher lets create it using the factory
		dataFetcher = getDataFetcherFactory().newMethodDataFetcher(this, null, method, fieldName.get(), dataFetcherClass);

		// field types of GraphQLList get a collection/map converter data fetcher by default
		// this data fetcher can be overridden below if the user specifies a custom datafetcher
		if (graphQLFieldType instanceof GraphQLList) {
			if (Collection.class.isAssignableFrom(fieldTypeClass)) {
				dataFetcher = new CollectionConverterDataFetcher(dataFetcher);
			} else if (Map.class.isAssignableFrom(fieldTypeClass)) {
				dataFetcher = new MapConverterDataFetcher(dataFetcher);
			}
		}
		builder.dataFetcher(dataFetcher);

		return Optional.of(builder.build());
	}

	private Optional<GraphQLFieldDefinition> getFieldType(Type type, Field field) {
		if (Modifier.isStatic(field.getModifiers())) {
			LOGGER.info("Ignoring types {} static field {}  ", type, field);
			return Optional.absent();
		}
		if ("this$0".equals(field.getName())) {
			// this is a dirty hack but we don't want to expose the parent pointer of inner classes...
			return Optional.absent();
		} else {
			LOGGER.info("Processing types {} field {}  ", type, field);
			try {
				Class<?> fieldTypeClass = getClassFromType(field.getGenericType());
				GraphQLOutputType graphQLFieldType = getOutputType(field.getGenericType());
				GraphQLFieldDefinition.Builder builder = GraphQLFieldDefinition.newFieldDefinition().name(field.getName()).type(graphQLFieldType);
				Class<? extends IDataFetcher> dataFetcherClass = null;

				// first check if it has a custom type mapper, if so attach the dataFetcher to the field if specified
				Optional<IGraphQLTypeMapper> typeMapper = getCustomTypeMapper(field.getGenericType());
				if (typeMapper.isPresent()) {
					GraphQLTypeMapper typeMapperAnnotation = typeMapper.get().getClass().getAnnotation(GraphQLTypeMapper.class);
					if (!AnnotationUtils.isNullValue(typeMapperAnnotation.dataFetcher())) {
						dataFetcherClass = typeMapperAnnotation.dataFetcher();
					}
				}

				// if the field type class is not null and we have an annotation on it for a custom datafetcher than use it
				if (fieldTypeClass != null) {
					GraphQLDataFetcher dataFetcherAnnotation = (GraphQLDataFetcher) fieldTypeClass.getAnnotation(GraphQLDataFetcher.class);
					if (dataFetcherAnnotation != null) {
						dataFetcherClass = dataFetcherAnnotation.dataFetcher();
					}
				}

				DataFetcher dataFetcher = new PropertyDataFetcher(field.getName());
				if (dataFetcherClass != null) {
					dataFetcher = getDataFetcherFactory().newFieldDataFetcher(this, field, dataFetcherClass);
				}
				// field types of GraphQLList get a collection/map converter data fetcher by default
				// this data fetcher can be overridden below if the user specifies a custom datafetcher
				if (graphQLFieldType instanceof GraphQLList) {
					if (Collection.class.isAssignableFrom(fieldTypeClass)) {
						dataFetcher = new CollectionConverterDataFetcher(dataFetcher);
					} else if (Map.class.isAssignableFrom(fieldTypeClass)) {
						dataFetcher = new MapConverterDataFetcher(dataFetcher);
					}
				}

				if (dataFetcher != null) {
					builder.dataFetcher(dataFetcher);
				}

				return Optional.of(builder.build());
			} catch (NotMappableException ex) {
				LOGGER.info("types field type {} not supported so ignored field named {}", type, field.getGenericType(), field.getName());
			}
		}
		return Optional.absent();
	}

	private Optional<IGraphQLTypeMapper> getCustomTypeMapper(Type type) {
		if (getClassTypeMappers().containsKey(type)) {
			return Optional.fromNullable(getClassTypeMappers().get(type));
		}
		// type variables can't have custom type mappers
		if (type instanceof TypeVariable) {
			return Optional.absent();
		}
		for (IGraphQLTypeMapper typeMapper : getInterfaceTypeMappers()) {

			if (typeMapper.handlesType(this, type)) {
				return Optional.of(typeMapper);
			}

		}

		return Optional.absent();
	}

	@Override
	public GraphQLInputType getInputType(Type type) {
		GraphQLInputType rv;
		String typeName = this.getTypeNamingStrategy().getTypeName(this, type);
		if (getInputTypeCache().containsKey(typeName)) {
			return getInputTypeCache().get(typeName);
		}
		// check typemapper
		Optional<IGraphQLTypeMapper> typeMapper = getCustomTypeMapper(type);
		if (typeMapper.isPresent()) {
			rv = getInputTypeCache().put(typeName, typeMapper.get().getInputType(this, type));
		} else {
			GraphQLOutputType outputType = getOutputType(type);
			rv = getInputTypeCache().put(typeName, getInputType(outputType));
		}

		return rv;
	}

	@Override
	public GraphQLOutputType getOutputType(Type type) {
		GraphQLOutputType rv;
		Class<?> classType;
		String typeName = this.getTypeNamingStrategy().getTypeName(this, type);

		if (getOutputTypeCache().containsKey(typeName)) {
			return getOutputTypeCache().get(typeName);
		}

		Optional<IGraphQLTypeMapper> typeMapper = getCustomTypeMapper(type);
		if (typeMapper.isPresent()) {
			rv = getOutputTypeCache().put(typeName, typeMapper.get().getOutputType(this, type));
		} else if (type instanceof ParameterizedType) {
			ParameterizedType parameterizedType = (ParameterizedType) type;
			Type rawType = parameterizedType.getRawType();
			Class<?> rawClass = (Class<?>) rawType;

			typeMapper = getCustomTypeMapper(rawClass);
			if (typeMapper.isPresent()) {
				rv = getOutputTypeCache().put(typeName, typeMapper.get().getOutputType(this, type));
			} else {
				return buildObject(type, rawClass);
			}
		} else if (type instanceof TypeVariable) {
			TypeVariable<?> vType = (TypeVariable<?>) type;
			return getOutputType(typeArguments.peek().get(vType.getName()));
		} else {
			classType = getClassFromType(type);
			Optional<GraphQLScalarType> graphQLType = getIfPrimitiveType(classType);

			if (graphQLType.isPresent()) {
				rv = getOutputTypeCache().put(typeName, graphQLType.get());
			} else if (classType.isEnum()) {
				GraphQLEnumType.Builder enumType = GraphQLEnumType.newEnum().name(typeNamingStrategy.getTypeName(this, type));
				for (Object value : classType.getEnumConstants()) {
					enumType.value(value.toString(), value);
				}
				rv = getOutputTypeCache().put(typeName, enumType.build());
			} else {
				rv = buildObject(type, classType);
			}
		}

		return rv;
	}

	@Override
	public IGraphQLTypeCache<GraphQLInputType> getInputTypeCache() {
		return inputTypeCache;
	}

	@Override
	public IGraphQLTypeCache<GraphQLOutputType> getOutputTypeCache() {
		return outputTypeCache;
	}

	@Override
    public ITypeFactory getTypeFactory() {
		return this.typeFactory;
	}

	private GraphQLInputType getInputType(GraphQLOutputType outputType) {
		if (GraphQLInputType.class.isAssignableFrom(outputType.getClass())) {
			return (GraphQLInputType) outputType;
		} else if (outputType instanceof GraphQLObjectType) {
			GraphQLObjectType objectType = (GraphQLObjectType) outputType;
			GraphQLInputObjectType.Builder rv = GraphQLInputObjectType.newInputObject().name(objectType.getName() + "_Input");

			for (GraphQLFieldDefinition field : objectType.getFieldDefinitions()) {
				rv.field(GraphQLInputObjectField.newInputObjectField().name(field.getName()).type(getInputType(field.getType())).build());
			}

			GraphQLInputType type = rv.build();
			inputTypes.add(type);
			return type;

		} else {
			throw new RuntimeException(String.format("Unknown output type %s", outputType.toString()));
		}
	}

	private Optional<GraphQLScalarType> getIfPrimitiveType(Class<?> classType) {
		GraphQLScalarType rv = null;
		// native types
		if (Integer.class.isAssignableFrom(classType) || classType.isAssignableFrom(int.class)) {
			rv = Scalars.GraphQLInt;
		} else if (Long.class.isAssignableFrom(classType) || long.class.isAssignableFrom(classType)) {
			rv = Scalars.GraphQLLong;
		} else if (Float.class.isAssignableFrom(classType) || float.class.isAssignableFrom(classType)) {
			rv = Scalars.GraphQLFloat;
		} else if (Double.class.isAssignableFrom(classType) || double.class.isAssignableFrom(classType)) {
			rv = Scalars.GraphQLFloat;
		} else if (String.class.isAssignableFrom(classType)) {
			rv = Scalars.GraphQLString;
		} else if (Boolean.class.isAssignableFrom(classType) || boolean.class.isAssignableFrom(classType)) {
			rv = Scalars.GraphQLBoolean;
		}

		return Optional.fromNullable(rv);
	}

	private GraphQLObjectType buildObject(Type type, Class<?> classType) {
		GraphQLObjectType rv;
		try {
			// object types we create an object type and then recursively call ourselves to get the field types
			String typeName = typeNamingStrategy.getTypeName(this, type);
			GraphQLObjectType.Builder glType = GraphQLObjectType.newObject().name(typeName);
			GraphQLTypeReference glTypeReference = new GraphQLTypeReference(typeName);
			ImmutableList.Builder<GraphQLFieldDefinition> fields = ImmutableList.builder();

			getOutputTypeCache().put(typeName, glTypeReference);
			Class<?> classItem = classType;
			Optional<GraphQLController> graphQLQueryable = Optional.fromNullable((GraphQLController) classItem.getAnnotation(GraphQLController.class));
			Optional<IQueryFactory> queryFactory = Optional.absent();
			Object objectInstance = null;

			// if it's queryable create a factory and instance of the object that we will execute queries upon
			if (graphQLQueryable.isPresent()) {
				queryFactory = Optional.of(graphQLQueryable.get().queryFactory().newInstance());
				objectInstance = classItem.newInstance();
			}
			// if we are a generic object then we need to build a generic variable to type mapping
			if (type instanceof ParameterizedType) {
				// if it's empty then we are at root generic class so create new type argument map for usage
				if (typeArguments.empty()) {
					typeArguments.push(Maps.<String, Type> newHashMap());
				} else {
					// we are inside the context of another generic object, so create a copy of parent map to use within new context
					typeArguments.push(Maps.newHashMap(typeArguments.peek()));
				}
				buildGenericArgumentTypeMap((ParameterizedType) type);
			}
			String classPackage = "";

			if (classItem != null && classItem.getPackage() != null) {
				classPackage = classItem.getPackage().getName();
			}
			// end when there are no more super classes and while ignore java.* types
			while (classItem != null && !classPackage.startsWith("java.")) {
				fields.addAll(getGraphQLFieldDefinitions(type, classItem));

				// pop currentContext
				classItem = classItem.getSuperclass();
				if (classItem != null && classItem.getPackage() != null) {
					classPackage = classItem.getPackage().getName();
				} else {
					classPackage = "";
				}

				if (queryFactory.isPresent()) {
					fields.addAll(queryFactory.get().newMethodQueriesForObject(this, objectInstance));
				}
			}

			// exiting context of current type arguments if we processed a generic type
			if (type instanceof ParameterizedType) {
				typeArguments.pop();
			}
			glType.fields(fields.build());

			// for classes that implement Node we need to declare them of type interface
			if (INode.class.isAssignableFrom(classType)) {
				glType.withInterface((GraphQLInterfaceType) getOutputTypeCache().get(nodeTypeName));
			}
			rv = (GraphQLObjectType) getOutputTypeCache().put(typeName, glType.build());
		} catch (InstantiationException | IllegalAccessException ex) {
			LOGGER.error("Unable to instantiate query factory for type class {}", classType.getName(), ex);
			Throwables.propagate(ex);
			rv = null; // never executed but gets rid of not initialized warning for rv;
		}

		return rv;
	}

	Collection<GraphQLFieldDefinition> getGraphQLFieldDefinitions(Type type, Class<?> classItem) {
		Map<String, GraphQLFieldDefinition> fieldDefinitions = Maps.newHashMap();
		Optional<GraphQLFieldDefinition> fieldDefinitionOptional;
		Set<String> ignoredFields = Sets.newHashSet();

		for (Method m : classItem.getDeclaredMethods()) {
			Optional<String> methodName = getFieldNameFromMethod(m);
			// we only look at getters and is types
			if (!methodName.isPresent()) {
				continue;
			}
			if (null != m.getAnnotation(GraphQLIgnore.class)) {
				ignoredFields.add(methodName.get());
				continue;
			}
			fieldDefinitionOptional = getFieldType(type, m);
			if (fieldDefinitionOptional.isPresent()) {
				fieldDefinitions.put(fieldDefinitionOptional.get().getName(), fieldDefinitionOptional.get());
			}
		}

		for (Field field : classItem.getDeclaredFields()) {
			// skip ignored fields
			if (null != field.getAnnotation(GraphQLIgnore.class) || ignoredFields.contains(field.getName())) {
				// if it's marked ignored then remove it.
				if (fieldDefinitions.containsKey(field.getName())) {
					fieldDefinitions.remove(field.getName());
				}
				continue;
			}
			if (fieldDefinitions.containsKey(field.getName())) {
				continue;
			}
			fieldDefinitionOptional = getFieldType(type, field);
			if (fieldDefinitionOptional.isPresent()) {
				if (!field.getName().startsWith("$")) {
					fieldDefinitions.put(fieldDefinitionOptional.get().getName(), fieldDefinitionOptional.get());
				}
			}
		}

		return fieldDefinitions.values();
	}

	@Override
	public ITypeNamingStrategy getTypeNamingStrategy() {
		return typeNamingStrategy;
	}

	public void setTypeNamingStrategy(ITypeNamingStrategy typeNamingStrategy) {
		this.typeNamingStrategy = typeNamingStrategy;
	}

	@VisibleForTesting
	ImmutableMap<Type, IGraphQLTypeMapper> getClassTypeMappers() {
		return classTypeMappers;
	}

	@VisibleForTesting
	ImmutableList<IGraphQLTypeMapper> getInterfaceTypeMappers() {
		return interfaceTypeMappers;
	}

	public List<Class<?>> getRelayNodeTypes() {
		return relayNodeTypes;
	}

	@Override
	public GraphQLObjectType getType(Object object) {
		return (GraphQLObjectType) getOutputType(object.getClass());
	}

	@Override
    public Class<?> getClassFromType(Type type) {

		if (type instanceof ParameterizedType) {
			return (Class<?>) ((ParameterizedType) type).getRawType();
		} else if (type instanceof TypeVariable) {
			return getClassFromType(typeArguments.peek().get(((TypeVariable<?>) type).getName()));
		} else if (type instanceof WildcardType) {
			// @TODO do a better job of wild card types here
			return getClassFromType(((WildcardType) type).getLowerBounds()[0]);
		} else {
			return (Class<?>) type;
		}
	}

	@Override
    public Set<GraphQLType> getInputTypes() {
		return inputTypes;
	}

	@Override
    public IDataFetcherFactory getDataFetcherFactory() {
		return dataFetcherFactory;
	}

	@Override
    public void setDataFetcherFactory(IDataFetcherFactory dataFetcherFactory) {
		this.dataFetcherFactory = dataFetcherFactory;
	}

	@Override
    public Class<? extends IDataFetcher> getDefaultMethodDataFetcher() {
		return defaultMethodDataFetcher;
	}

	@Override
    public void setDefaultMethodDataFetcher(Class<? extends IDataFetcher> defaultMethodDataFetcher) {
		this.defaultMethodDataFetcher = defaultMethodDataFetcher;
	}
}
