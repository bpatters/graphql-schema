package com.bretpatterson.schemagen.graphql.relay;

import java.util.List;

/**
 * Generic class used for representing Relay Connection Objects
 */
public class RelayConnection<T> {
	private List<Edge<T>> edges;
	private PageInfo pageInfo;

	public List<Edge<T>> getEdges() {
		return edges;
	}

	public RelayConnection<T> setEdges(List<Edge<T>> edges) {
		this.edges = edges;

		return this;
	}

	public PageInfo getPageInfo() {
		return pageInfo;
	}

	public RelayConnection<T> setPageInfo(PageInfo pageInfo) {
		this.pageInfo = pageInfo;

		return this;
	}
}
