package com.bretpatterson.schemagen.graphql.relay.exceptions;

/**
 * When constructing Node objects this is thrown when we are unable to construct the object from the specified ID.
 */
public class UnknownObjectType extends RuntimeException {
    private static final long serialVersionUID = -5858938084329702803L;

    public UnknownObjectType(String message) {
		super(message);
	}
}
