package com.bretpatterson.schemagen.graphql.impl;

import com.bretpatterson.schemagen.graphql.GraphQLSchemaBuilder;
import com.bretpatterson.schemagen.graphql.IDataFetcherFactory;
import com.bretpatterson.schemagen.graphql.ITypeNamingStrategy;
import com.bretpatterson.schemagen.graphql.ITypeFactory;
import com.bretpatterson.schemagen.graphql.datafetchers.IDataFetcher;
import com.bretpatterson.schemagen.graphql.datafetchers.MapConverterDataFetcher;
import com.bretpatterson.schemagen.graphql.typemappers.java.util.MapMapper;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.reflect.TypeToken;
import graphql.Scalars;
import graphql.schema.GraphQLEnumType;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLOutputType;
import graphql.schema.GraphQLType;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.Vector;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Tests that generic version of the following collection implements can be mapped correctly.
 * 
 * @see java.util.ArrayList
 * @see java.util.LinkedList
 * @see java.util.HashSet
 * @see java.util.TreeSet
 * @see java.util.Vector
 */
public class GraphQLObjectMapper_CollectionsTest {

	@Mock
	ITypeFactory objectMapper;
	GraphQLObjectMapper graphQLObjectMapper;
	GraphQLObjectType expectedObjectType;

	private enum TestEnum {
		KEY1, KEY2, KEY3
	}

	private class CollectionsTestObject {
		@SuppressWarnings("unused")
        String field1;
		@SuppressWarnings("unused")
        Integer field2;
	}

	@Before
	public void setup() {
		expectedObjectType = GraphQLObjectType.newObject()
				.name(CollectionsTestObject.class.getSimpleName())
				.field(GraphQLFieldDefinition.newFieldDefinition().name("field1").type(Scalars.GraphQLString).build())
				.field(GraphQLFieldDefinition.newFieldDefinition().name("field2").type(Scalars.GraphQLInt).build())
				.build();

		graphQLObjectMapper = new GraphQLObjectMapper(objectMapper,
				GraphQLSchemaBuilder.getDefaultTypeMappers(),
				Optional.<ITypeNamingStrategy>absent(),
				Optional.<IDataFetcherFactory>absent(),
				Optional.<Class<? extends IDataFetcher>>absent(),
				ImmutableList.<Class<?>> of());
	}

	private void assertGenericListTypeMapping(String name, GraphQLOutputType expectedWrappedType, GraphQLOutputType graphQLOutputType) {
		assertEquals(GraphQLList.class, graphQLOutputType.getClass());

		GraphQLList listType = (GraphQLList) graphQLOutputType;
		GraphQLType wrappedType = listType.getWrappedType();

		assertEquals(name, wrappedType.getName());
		assertEquals(expectedWrappedType.getClass(), wrappedType.getClass());

		if (expectedWrappedType instanceof GraphQLObjectType) {
			GraphQLObjectType objectType = (GraphQLObjectType) wrappedType;

			assertEquals("field1", objectType.getFieldDefinition("field1").getName());
			assertEquals(Scalars.GraphQLString, objectType.getFieldDefinition("field1").getType());
			assertEquals("field2", objectType.getFieldDefinition("field2").getName());
			assertEquals(Scalars.GraphQLInt, objectType.getFieldDefinition("field2").getType());
		}
	}

	private void assertGenericMapTypeMapping(String name, GraphQLOutputType expectedValueType, GraphQLOutputType graphQLOutputType) {

		GraphQLList listType = (GraphQLList) graphQLOutputType;

		// verify we contstructed an object from the map with the enum values as fields
		// and their type the expected value type
		assertEquals(GraphQLList.class, listType.getClass());
		GraphQLObjectType objectType = (GraphQLObjectType) listType.getWrappedType();

		assertEquals(name, objectType.getName());
		assertNotNull(objectType.getFieldDefinition(MapMapper.KEY_NAME));
		assertNotNull(objectType.getFieldDefinition(MapMapper.VALUE_NAME));
		assertEquals(expectedValueType.getClass(), objectType.getFieldDefinition(MapMapper.KEY_NAME).getType().getClass());
		assertEquals(expectedValueType.getClass(), objectType.getFieldDefinition(MapMapper.VALUE_NAME).getType().getClass());
	}

	@SuppressWarnings("serial")
    @Test
	public void testGenericCollectionsMapping() {

		assertGenericListTypeMapping(Scalars.GraphQLString.getName(),
				Scalars.GraphQLString,
				graphQLObjectMapper.getOutputType(new TypeToken<Collection<String>>() {
				}.getType()));
		assertGenericListTypeMapping(Scalars.GraphQLInt.getName(), Scalars.GraphQLInt, graphQLObjectMapper.getOutputType(new TypeToken<ArrayList<Integer>>() {
		}.getType()));
		assertGenericListTypeMapping(Scalars.GraphQLLong.getName(), Scalars.GraphQLLong, graphQLObjectMapper.getOutputType(new TypeToken<Collection<Long>>() {
		}.getType()));
		assertGenericListTypeMapping(Scalars.GraphQLFloat.getName(),
				Scalars.GraphQLFloat,
				graphQLObjectMapper.getOutputType(new TypeToken<Collection<Float>>() {
				}.getType()));
		assertGenericListTypeMapping(Scalars.GraphQLFloat.getName(),
				Scalars.GraphQLFloat,
				graphQLObjectMapper.getOutputType(new TypeToken<Collection<Double>>() {
				}.getType()));
		assertGenericListTypeMapping(Scalars.GraphQLBoolean.getName(),
				Scalars.GraphQLBoolean,
				graphQLObjectMapper.getOutputType(new TypeToken<Collection<Boolean>>() {
				}.getType()));
		assertGenericListTypeMapping(expectedObjectType.getName(),
				expectedObjectType,
				graphQLObjectMapper.getOutputType(new TypeToken<Collection<CollectionsTestObject>>() {
				}.getType()));
	}

	@SuppressWarnings("serial")
    @Test
	public void testGenericListMappings() {

		assertGenericListTypeMapping(Scalars.GraphQLString.getName(), Scalars.GraphQLString, graphQLObjectMapper.getOutputType(new TypeToken<List<String>>() {
		}.getType()));
		assertGenericListTypeMapping(Scalars.GraphQLInt.getName(), Scalars.GraphQLInt, graphQLObjectMapper.getOutputType(new TypeToken<List<Integer>>() {
		}.getType()));
		assertGenericListTypeMapping(Scalars.GraphQLLong.getName(), Scalars.GraphQLLong, graphQLObjectMapper.getOutputType(new TypeToken<List<Long>>() {
		}.getType()));
		assertGenericListTypeMapping(Scalars.GraphQLFloat.getName(), Scalars.GraphQLFloat, graphQLObjectMapper.getOutputType(new TypeToken<List<Float>>() {
		}.getType()));
		assertGenericListTypeMapping(Scalars.GraphQLFloat.getName(), Scalars.GraphQLFloat, graphQLObjectMapper.getOutputType(new TypeToken<List<Double>>() {
		}.getType()));
		assertGenericListTypeMapping(Scalars.GraphQLBoolean.getName(),
				Scalars.GraphQLBoolean,
				graphQLObjectMapper.getOutputType(new TypeToken<List<Boolean>>() {
				}.getType()));
		assertGenericListTypeMapping(expectedObjectType.getName(),
				expectedObjectType,
				graphQLObjectMapper.getOutputType(new TypeToken<List<CollectionsTestObject>>() {
				}.getType()));
	}

	@SuppressWarnings("serial")
    @Test
	public void testGenericSetMappings() {

		assertGenericListTypeMapping(Scalars.GraphQLString.getName(), Scalars.GraphQLString, graphQLObjectMapper.getOutputType(new TypeToken<List<String>>() {
		}.getType()));
		assertGenericListTypeMapping(Scalars.GraphQLInt.getName(), Scalars.GraphQLInt, graphQLObjectMapper.getOutputType(new TypeToken<List<Integer>>() {
		}.getType()));
		assertGenericListTypeMapping(Scalars.GraphQLLong.getName(), Scalars.GraphQLLong, graphQLObjectMapper.getOutputType(new TypeToken<List<Long>>() {
		}.getType()));
		assertGenericListTypeMapping(Scalars.GraphQLFloat.getName(), Scalars.GraphQLFloat, graphQLObjectMapper.getOutputType(new TypeToken<List<Float>>() {
		}.getType()));
		assertGenericListTypeMapping(Scalars.GraphQLFloat.getName(), Scalars.GraphQLFloat, graphQLObjectMapper.getOutputType(new TypeToken<List<Double>>() {
		}.getType()));
		assertGenericListTypeMapping(Scalars.GraphQLBoolean.getName(),
				Scalars.GraphQLBoolean,
				graphQLObjectMapper.getOutputType(new TypeToken<List<Boolean>>() {
				}.getType()));
		assertGenericListTypeMapping(expectedObjectType.getName(),
				expectedObjectType,
				graphQLObjectMapper.getOutputType(new TypeToken<List<CollectionsTestObject>>() {
				}.getType()));
	}

	@SuppressWarnings("serial")
    @Test
	public void testGenericArrayListMapping() {

		assertGenericListTypeMapping(Scalars.GraphQLString.getName(),
				Scalars.GraphQLString,
				graphQLObjectMapper.getOutputType(new TypeToken<ArrayList<String>>() {
				}.getType()));
		assertGenericListTypeMapping(Scalars.GraphQLInt.getName(), Scalars.GraphQLInt, graphQLObjectMapper.getOutputType(new TypeToken<ArrayList<Integer>>() {
		}.getType()));
		assertGenericListTypeMapping(Scalars.GraphQLLong.getName(), Scalars.GraphQLLong, graphQLObjectMapper.getOutputType(new TypeToken<ArrayList<Long>>() {
		}.getType()));
		assertGenericListTypeMapping(Scalars.GraphQLFloat.getName(), Scalars.GraphQLFloat, graphQLObjectMapper.getOutputType(new TypeToken<ArrayList<Float>>() {
		}.getType()));
		assertGenericListTypeMapping(Scalars.GraphQLFloat.getName(),
				Scalars.GraphQLFloat,
				graphQLObjectMapper.getOutputType(new TypeToken<ArrayList<Double>>() {
				}.getType()));
		assertGenericListTypeMapping(Scalars.GraphQLBoolean.getName(),
				Scalars.GraphQLBoolean,
				graphQLObjectMapper.getOutputType(new TypeToken<ArrayList<Boolean>>() {
				}.getType()));
		assertGenericListTypeMapping(expectedObjectType.getName(),
				expectedObjectType,
				graphQLObjectMapper.getOutputType(new TypeToken<ArrayList<CollectionsTestObject>>() {
				}.getType()));
	}

	@SuppressWarnings("serial")
    @Test
	public void testGenericLinkedListMapping() {

		assertGenericListTypeMapping(Scalars.GraphQLString.getName(),
				Scalars.GraphQLString,
				graphQLObjectMapper.getOutputType(new TypeToken<LinkedList<String>>() {
				}.getType()));
		assertGenericListTypeMapping(Scalars.GraphQLInt.getName(), Scalars.GraphQLInt, graphQLObjectMapper.getOutputType(new TypeToken<LinkedList<Integer>>() {
		}.getType()));
		assertGenericListTypeMapping(Scalars.GraphQLLong.getName(), Scalars.GraphQLLong, graphQLObjectMapper.getOutputType(new TypeToken<LinkedList<Long>>() {
		}.getType()));
		assertGenericListTypeMapping(Scalars.GraphQLFloat.getName(),
				Scalars.GraphQLFloat,
				graphQLObjectMapper.getOutputType(new TypeToken<LinkedList<Float>>() {
				}.getType()));
		assertGenericListTypeMapping(Scalars.GraphQLFloat.getName(),
				Scalars.GraphQLFloat,
				graphQLObjectMapper.getOutputType(new TypeToken<LinkedList<Double>>() {
				}.getType()));
		assertGenericListTypeMapping(Scalars.GraphQLBoolean.getName(),
				Scalars.GraphQLBoolean,
				graphQLObjectMapper.getOutputType(new TypeToken<LinkedList<Boolean>>() {
				}.getType()));
		assertGenericListTypeMapping(expectedObjectType.getName(),
				expectedObjectType,
				graphQLObjectMapper.getOutputType(new TypeToken<LinkedList<CollectionsTestObject>>() {
				}.getType()));
	}

	@SuppressWarnings("serial")
    @Test
	public void testGenericHashSetMapping() {

		assertGenericListTypeMapping(Scalars.GraphQLString.getName(),
				Scalars.GraphQLString,
				graphQLObjectMapper.getOutputType(new TypeToken<HashSet<String>>() {
				}.getType()));
		assertGenericListTypeMapping(Scalars.GraphQLInt.getName(), Scalars.GraphQLInt, graphQLObjectMapper.getOutputType(new TypeToken<HashSet<Integer>>() {
		}.getType()));
		assertGenericListTypeMapping(Scalars.GraphQLLong.getName(), Scalars.GraphQLLong, graphQLObjectMapper.getOutputType(new TypeToken<HashSet<Long>>() {
		}.getType()));
		assertGenericListTypeMapping(Scalars.GraphQLFloat.getName(), Scalars.GraphQLFloat, graphQLObjectMapper.getOutputType(new TypeToken<HashSet<Float>>() {
		}.getType()));
		assertGenericListTypeMapping(Scalars.GraphQLFloat.getName(), Scalars.GraphQLFloat, graphQLObjectMapper.getOutputType(new TypeToken<HashSet<Double>>() {
		}.getType()));
		assertGenericListTypeMapping(Scalars.GraphQLBoolean.getName(),
				Scalars.GraphQLBoolean,
				graphQLObjectMapper.getOutputType(new TypeToken<HashSet<Boolean>>() {
				}.getType()));
		assertGenericListTypeMapping(expectedObjectType.getName(),
				expectedObjectType,
				graphQLObjectMapper.getOutputType(new TypeToken<HashSet<CollectionsTestObject>>() {
				}.getType()));
	}

	@SuppressWarnings("serial")
    @Test
	public void testGenericTreeSetMapping() {

		assertGenericListTypeMapping(Scalars.GraphQLString.getName(),
				Scalars.GraphQLString,
				graphQLObjectMapper.getOutputType(new TypeToken<TreeSet<String>>() {
				}.getType()));
		assertGenericListTypeMapping(Scalars.GraphQLInt.getName(), Scalars.GraphQLInt, graphQLObjectMapper.getOutputType(new TypeToken<TreeSet<Integer>>() {
		}.getType()));
		assertGenericListTypeMapping(Scalars.GraphQLLong.getName(), Scalars.GraphQLLong, graphQLObjectMapper.getOutputType(new TypeToken<TreeSet<Long>>() {
		}.getType()));
		assertGenericListTypeMapping(Scalars.GraphQLFloat.getName(), Scalars.GraphQLFloat, graphQLObjectMapper.getOutputType(new TypeToken<TreeSet<Float>>() {
		}.getType()));
		assertGenericListTypeMapping(Scalars.GraphQLFloat.getName(), Scalars.GraphQLFloat, graphQLObjectMapper.getOutputType(new TypeToken<TreeSet<Double>>() {
		}.getType()));
		assertGenericListTypeMapping(Scalars.GraphQLBoolean.getName(),
				Scalars.GraphQLBoolean,
				graphQLObjectMapper.getOutputType(new TypeToken<TreeSet<Boolean>>() {
				}.getType()));
		assertGenericListTypeMapping(expectedObjectType.getName(),
				expectedObjectType,
				graphQLObjectMapper.getOutputType(new TypeToken<TreeSet<CollectionsTestObject>>() {
				}.getType()));
	}



	@SuppressWarnings("serial")
    @Test
	public void testGenericVectorMapping() {

		assertGenericListTypeMapping(Scalars.GraphQLString.getName(), Scalars.GraphQLString, graphQLObjectMapper.getOutputType(new TypeToken<Vector<String>>() {
		}.getType()));
		assertGenericListTypeMapping(Scalars.GraphQLInt.getName(), Scalars.GraphQLInt, graphQLObjectMapper.getOutputType(new TypeToken<Vector<Integer>>() {
		}.getType()));
		assertGenericListTypeMapping(Scalars.GraphQLLong.getName(), Scalars.GraphQLLong, graphQLObjectMapper.getOutputType(new TypeToken<Vector<Long>>() {
		}.getType()));
		assertGenericListTypeMapping(Scalars.GraphQLFloat.getName(), Scalars.GraphQLFloat, graphQLObjectMapper.getOutputType(new TypeToken<Vector<Float>>() {
		}.getType()));
		assertGenericListTypeMapping(Scalars.GraphQLFloat.getName(), Scalars.GraphQLFloat, graphQLObjectMapper.getOutputType(new TypeToken<Vector<Double>>() {
		}.getType()));
		assertGenericListTypeMapping(Scalars.GraphQLBoolean.getName(),
				Scalars.GraphQLBoolean,
				graphQLObjectMapper.getOutputType(new TypeToken<Vector<Boolean>>() {
				}.getType()));
		assertGenericListTypeMapping(expectedObjectType.getName(),
				expectedObjectType,
				graphQLObjectMapper.getOutputType(new TypeToken<Vector<CollectionsTestObject>>() {
				}.getType()));
	}



	private class GenericMapTestObject {
		@SuppressWarnings("unused")
        Map<String,String> mapField;
	}
	@SuppressWarnings("serial")
    @Test
	public void testGenericMapTypeMapping() {

		assertGenericMapTypeMapping("Map_String_String", Scalars.GraphQLString, graphQLObjectMapper.getOutputType(new TypeToken<Map<String, String>>() {
		}.getType()));
		assertGenericMapTypeMapping("Map_Integer_Integer", Scalars.GraphQLInt, graphQLObjectMapper.getOutputType(new TypeToken<Map<Integer, Integer>>() {
		}.getType()));
		assertGenericMapTypeMapping("Map_Long_Long", Scalars.GraphQLLong, graphQLObjectMapper.getOutputType(new TypeToken<Map<Long, Long>>() {
		}.getType()));
		assertGenericMapTypeMapping("Map_Float_Float", Scalars.GraphQLFloat, graphQLObjectMapper.getOutputType(new TypeToken<Map<Float, Float>>() {
		}.getType()));
		assertGenericMapTypeMapping("Map_Double_Double", Scalars.GraphQLFloat, graphQLObjectMapper.getOutputType(new TypeToken<Map<Double, Double>>() {
		}.getType()));
		assertGenericMapTypeMapping("Map_Boolean_Boolean", Scalars.GraphQLBoolean, graphQLObjectMapper.getOutputType(new TypeToken<Map<Boolean, Boolean>>() {
		}.getType()));
		assertGenericMapTypeMapping("Map_CollectionsTestObject_CollectionsTestObject",
				expectedObjectType,
				graphQLObjectMapper.getOutputType(new TypeToken<Map<CollectionsTestObject, CollectionsTestObject>>() {
				}.getType()));

		GraphQLObjectType objectType = (GraphQLObjectType) graphQLObjectMapper.getOutputType(GenericMapTestObject.class);

		assertNotNull(objectType.getFieldDefinition("mapField").getDataFetcher());
		assertEquals(MapConverterDataFetcher.class, objectType.getFieldDefinition("mapField").getDataFetcher().getClass());

	}

	@SuppressWarnings("serial")
    @Test
	public void testRecursiveTypes() {
		GraphQLList listType = (GraphQLList) graphQLObjectMapper
				.getOutputType(new TypeToken<Map<TestEnum, List<List<List<List<List<List<String>>>>>>>>() {
				}.getType());
		GraphQLObjectType outputType = (GraphQLObjectType) listType.getWrappedType();

		assertEquals("Map_TestEnum_List_List_List_List_List_List_String", outputType.getName());

		GraphQLEnumType keyType = (GraphQLEnumType) outputType.getFieldDefinition(MapMapper.KEY_NAME).getType();
		GraphQLType valueType = outputType.getFieldDefinition(MapMapper.VALUE_NAME).getType();
		int depth = 0;
		while (valueType.getClass() == GraphQLList.class) {
			depth++;
			valueType = ((GraphQLList) valueType).getWrappedType();
		}
		assertEquals(6, depth);

		// now we verify the key type
		assertEquals(Scalars.GraphQLString, valueType);
	}

}
