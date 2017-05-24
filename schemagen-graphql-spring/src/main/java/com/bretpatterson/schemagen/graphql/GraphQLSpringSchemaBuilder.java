package com.bretpatterson.schemagen.graphql;

import com.bretpatterson.schemagen.graphql.annotations.GraphQLTypeMapper;
import com.bretpatterson.schemagen.graphql.datafetchers.spring.SpringDataFetcherFactory;
import com.bretpatterson.schemagen.graphql.typemappers.IGraphQLTypeMapper;
import com.google.common.collect.ImmutableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.util.ClassUtils;

import java.util.List;

public class GraphQLSpringSchemaBuilder extends GraphQLSchemaBuilder {

    private static final Logger LOGGER = LoggerFactory.getLogger(GraphQLSpringSchemaBuilder.class);

    private final ApplicationContext applicationContext;

    public GraphQLSpringSchemaBuilder(ApplicationContext applicationContext) {
        super();

        this.applicationContext = applicationContext;
        this.registerDataFetcherFactory(new SpringDataFetcherFactory(applicationContext));
        this.registerTypeMappers(getDefaultTypeMappers());
    }

    public static List<IGraphQLTypeMapper> getDefaultTypeMappers() {
        ImmutableList.Builder<IGraphQLTypeMapper> builder = ImmutableList.builder();

        ClassPathScanningCandidateComponentProvider scanner =
                new ClassPathScanningCandidateComponentProvider(true);

        scanner.addIncludeFilter(new AnnotationTypeFilter(GraphQLTypeMapper.class));

        for (BeanDefinition bd : scanner.findCandidateComponents(IGraphQLTypeMapper.class.getPackage().getName())) {
            try {
                Class<?> cls = ClassUtils.resolveClassName(bd.getBeanClassName(),
                        ClassUtils.getDefaultClassLoader());

                builder.add((IGraphQLTypeMapper) cls.newInstance());
            } catch (Exception e) {
                LOGGER.error("Unexpected exception.", e);
            }
        }
        return builder.build();
    }
}
