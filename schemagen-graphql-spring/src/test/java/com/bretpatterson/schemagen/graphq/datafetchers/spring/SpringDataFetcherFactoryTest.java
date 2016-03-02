package com.bretpatterson.schemagen.graphq.datafetchers.spring;

import com.bretpatterson.schemagen.graphql.GraphQLSchemaBuilder;
import com.bretpatterson.schemagen.graphql.IDataFetcherFactory;
import com.bretpatterson.schemagen.graphql.ITypeNamingStrategy;
import com.bretpatterson.schemagen.graphql.annotations.GraphQLParam;
import com.bretpatterson.schemagen.graphql.annotations.GraphQLSpringELDataFetcher;
import com.bretpatterson.schemagen.graphql.datafetchers.DefaultMethodDataFetcher;
import com.bretpatterson.schemagen.graphql.datafetchers.IDataFetcher;
import com.bretpatterson.schemagen.graphql.datafetchers.spring.SpringDataFetcherFactory;
import com.bretpatterson.schemagen.graphql.impl.GraphQLObjectMapper;
import com.bretpatterson.schemagen.graphql.impl.SimpleTypeFactory;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import graphql.language.Field;
import graphql.schema.DataFetchingEnvironment;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.junit.Assert.assertEquals;
import static org.mockito.BDDMockito.willReturn;
import static org.mockito.Mockito.mock;

/**
 * Created by bpatterson on 3/1/16.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {TestModule.class})
public class SpringDataFetcherFactoryTest {
	@Autowired
	ApplicationContext context;
	@GraphQLSpringELDataFetcher(value="@echoBean.echo('Hello World!')")
	String fieldTest = "Invalid Value!";


	@GraphQLSpringELDataFetcher("@echoBean.echo(#param1, #param2)")
	public String getData(@GraphQLParam(name="param1") String param1, @GraphQLParam(name="param2") Integer param2) {
			return "data value";
	}

	public String getDataNoSpring(@GraphQLParam(name="param1") String param1, @GraphQLParam(name="param2") Integer param2) {
		return "data value";
	}

	@Test
	public void testDefaultDataFetcher() throws NoSuchMethodException {
		GraphQLObjectMapper objectMapper = new GraphQLObjectMapper(new SimpleTypeFactory(),
																   GraphQLSchemaBuilder.getDefaultTypeMappers(),
																   Optional.<ITypeNamingStrategy>absent(),
																   Optional.<IDataFetcherFactory>absent(),
																   Optional.<Class<? extends IDataFetcher>>absent(),
																   ImmutableList.<Class<?>>of());
		SpringDataFetcherFactory factory = new SpringDataFetcherFactory(context);

		IDataFetcher dataFetcher = factory.newMethodDataFetcher(objectMapper, this, this.getClass().getMethod("getDataNoSpring", String.class, Integer.class), "dataNoSpring", DefaultMethodDataFetcher.class);
		DataFetchingEnvironment environment = mock(DataFetchingEnvironment.class);
		Field field = mock(Field.class);

		willReturn(this).given(environment).getSource();
		willReturn("dataNoSpring").given(field).getName();
		willReturn(ImmutableList.of(field)).given(environment).getFields();
		willReturn("value1").given(environment).getArgument("param1");
		willReturn(1234).given(environment).getArgument("param2");

		String value = (String) dataFetcher.get(environment);

		assertEquals("data value",value);
	}

	@Test
	public void testCanCreateDataFetcherForMethod() throws NoSuchMethodException {
		GraphQLObjectMapper objectMapper = new GraphQLObjectMapper(new SimpleTypeFactory(),
																   GraphQLSchemaBuilder.getDefaultTypeMappers(),
																   Optional.<ITypeNamingStrategy>absent(),
																   Optional.<IDataFetcherFactory>absent(),
																   Optional.<Class<? extends IDataFetcher>>absent(),
																   ImmutableList.<Class<?>>of());
		SpringDataFetcherFactory factory = new SpringDataFetcherFactory(context);
		EchoBean echoBean = (EchoBean) context.getBean("echoBean");

		IDataFetcher dataFetcher = factory.newMethodDataFetcher(objectMapper, this, this.getClass().getMethod("getData", String.class, Integer.class), "data", null);
		DataFetchingEnvironment environment = mock(DataFetchingEnvironment.class);
		Field field = mock(Field.class);

		willReturn(this).given(environment).getSource();
		willReturn("data").given(field).getName();
		willReturn(ImmutableList.of(field)).given(environment).getFields();
		willReturn("value1").given(environment).getArgument("param1");
		willReturn(1234).given(environment).getArgument("param2");

		String value = (String) dataFetcher.get(environment);

		assertEquals(echoBean.echo("value1",1234), value);
	}

	@Test
	public void testCanCreateDataFetcherForField() throws NoSuchFieldException {
		GraphQLObjectMapper objectMapper = new GraphQLObjectMapper(new SimpleTypeFactory(),
				GraphQLSchemaBuilder.getDefaultTypeMappers(),
				Optional.<ITypeNamingStrategy> absent(),
				Optional.<IDataFetcherFactory> absent(),
				Optional.<Class<? extends IDataFetcher>> absent(),
				ImmutableList.<Class<?>>of());
		SpringDataFetcherFactory factory = new SpringDataFetcherFactory(context);
		EchoBean echoBean = (EchoBean) context.getBean("echoBean");

		IDataFetcher dataFetcher = factory.newFieldDataFetcher(objectMapper, this.getClass().getDeclaredField("fieldTest"), null);
		DataFetchingEnvironment environment = mock(DataFetchingEnvironment.class);
		Field field = mock(Field.class);

		willReturn(this).given(environment).getSource();
		willReturn("fieldTest").given(field).getName();
		willReturn(ImmutableList.of(field)).given(environment).getFields();

		String value = (String) dataFetcher.get(environment);

		assertEquals(echoBean.echo("Hello World!"), value);
	}


}


