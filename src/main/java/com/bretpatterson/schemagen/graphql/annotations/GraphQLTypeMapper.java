package com.bretpatterson.schemagen.graphql.annotations;

import com.bretpatterson.schemagen.graphql.datafetchers.IDataFetcher;
import com.bretpatterson.schemagen.graphql.utils.AnnotationUtils;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to use to configure a default type mapper for a type
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface GraphQLTypeMapper{

	/**
	 * The Class this type mapper knows how to handle.
	 * @return
	 */
	Class<?> type();


	/**
	 * Allows you to override the default datafetcher for this data type.
	 * @return
	 */
	Class<? extends IDataFetcher> dataFetcher() default AnnotationUtils.DEFAULT_NULL_CLASS.class;
}
