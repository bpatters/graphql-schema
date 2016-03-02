package com.bretpatterson.schemagen.graphql.exceptions;

/**
 * Created by bpatterson on 1/23/16.
 */
public class NotMappableException extends RuntimeException {
    private static final long serialVersionUID = -8042949483199730066L;

    public NotMappableException(String message) {
		super(message);
	}
}
