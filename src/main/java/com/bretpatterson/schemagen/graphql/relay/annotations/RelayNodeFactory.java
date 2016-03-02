package com.bretpatterson.schemagen.graphql.relay.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation should be used for Relay Node Factory methods that know
 * how to process node requests.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface RelayNodeFactory {

	/**
	 * Types of objects this node factory implements.
	 * @return
	 */
	Class<?>[] types();
}
