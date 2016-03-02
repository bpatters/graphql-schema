package com.bretpatterson.schemagen.graphql.relay;


import java.util.Objects;

/**
 * Generic type for representing a Relay Edge connection.
 */
public class Edge<T> {

	T node;
	ConnectionCursor cursor;

	public Edge() {

	}

	public Edge(T node, ConnectionCursor cursor) {
		this.node = node;
		this.cursor = cursor;
	}

	public T getNode() {
		return node;
	}

	public void setNode(T node) {
		this.node = node;
	}

	public ConnectionCursor getCursor() {
		return cursor;
	}

	public void setCursor(ConnectionCursor cursor) {
		this.cursor = cursor;
	}

	@Override
	public boolean equals(Object o) {

		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;

		Edge<?> that = (Edge<?>) o;

		return Objects.equals(node, that.node) && Objects.equals(cursor, that.cursor);

	}

	@Override
	public int hashCode() {
		return Objects.hash(node, cursor);
	}
}
