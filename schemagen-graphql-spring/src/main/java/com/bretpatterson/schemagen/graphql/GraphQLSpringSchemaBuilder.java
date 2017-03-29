package com.bretpatterson.schemagen.graphql;

import com.bretpatterson.schemagen.graphql.annotations.GraphQLTypeMapper;
import com.bretpatterson.schemagen.graphql.datafetchers.spring.SpringDataFetcherFactory;
import com.bretpatterson.schemagen.graphql.typemappers.IGraphQLTypeMapper;
import com.google.common.collect.ImmutableList;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.util.ClassUtils;

import java.util.List;

public class GraphQLSpringSchemaBuilder extends GraphQLSchemaBuilder {

    private final ApplicationContext applicationContext;

    public GraphQLSpringSchemaBuilder(ApplicationContext applicationContext) {
        super();

        this.applicationContext = applicationContext;
        this.registerDataFetcherFactory(new SpringDataFetcherFactory(applicationContext));
    }

    public List<IGraphQLTypeMapper> getDefaultTypeMappers() {
        ImmutableList.Builder<IGraphQLTypeMapper> builder = ImmutableList.builder();

        ClassPathScanningCandidateComponentProvider scanner =
                new ClassPathScanningCandidateComponentProvider(true);

        scanner.addIncludeFilter(new AnnotationTypeFilter(GraphQLTypeMapper.class));

        for (BeanDefinition bd : scanner.findCandidateComponents(IGraphQLTypeMapper.class.getPackage().getName())) {
            try {
                Class<?> cls = ClassUtils.resolveClassName(bd.getBeanClassName(),
                        ClassUtils.getDefaultClassLoader());

                builder.add((IGraphQLTypeMapper) cls.newInstance());
            } catch (InstantiationException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        return builder.build();
    }
}
