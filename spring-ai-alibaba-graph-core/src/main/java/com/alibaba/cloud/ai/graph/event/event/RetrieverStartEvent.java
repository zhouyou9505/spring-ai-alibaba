package com.alibaba.cloud.ai.graph.event.event;

import com.alibaba.cloud.ai.graph.event.type.EventType;

/**
 * An event that represents the initiation of a retriever operation.
 * <p>
 * This event is fired when a retriever begins its operation lifecycle. It provides
 * essential tracking information including retriever identification and query context to
 * enable monitoring and correlation with subsequent events in the retriever's execution.
 * </p>
 * <p>
 * The event automatically sets its type to {@link EventType#RETRIEVER_START} and provides
 * fields to store retriever metadata for tracking purposes.
 * </p>
 *
 * @see com.alibaba.cloud.ai.graph.event.event.BaseEvent
 * @see EventType#RETRIEVER_START
 * @see RetrieverFinishedEvent
 * @see RetrieverErrorEvent
 * @author System Generated
 */
public class RetrieverStartEvent extends BaseEvent {

	private String retrieverId;

	private String retrieverName;

	private String query;

	private Integer topK;

	/**
	 * Creates a new RetrieverStartEvent with type set to {@link EventType#RETRIEVER_START}.
	 * <p>
	 * The timestamp is automatically set to the current time and all identifier fields
	 * are initialized as null.
	 * </p>
	 */
	public RetrieverStartEvent() {
		super(EventType.RETRIEVER_START);
	}

	/**
	 * Sets the unique identifier for the retriever that is starting.
	 * @param retrieverId the retriever identifier. Can be null.
	 */
	public void setRetrieverId(final String retrieverId) {
		this.retrieverId = retrieverId;
	}

	/**
	 * Returns the unique identifier for the retriever that is starting.
	 * @return the retriever identifier, can be null
	 */
	public String getRetrieverId() {
		return this.retrieverId;
	}

	/**
	 * Sets the name of the retriever that is starting.
	 * @param retrieverName the retriever name. Can be null.
	 */
	public void setRetrieverName(final String retrieverName) {
		this.retrieverName = retrieverName;
	}

	/**
	 * Returns the name of the retriever that is starting.
	 * @return the retriever name, can be null
	 */
	public String getRetrieverName() {
		return this.retrieverName;
	}

	/**
	 * Sets the query text for the retrieval operation.
	 * @param query the query text. Can be null.
	 */
	public void setQuery(final String query) {
		this.query = query;
	}

	/**
	 * Returns the query text for the retrieval operation.
	 * @return the query text, can be null
	 */
	public String getQuery() {
		return this.query;
	}

	/**
	 * Sets the top-K parameter for the retrieval operation.
	 * @param topK the top-K value. Can be null.
	 */
	public void setTopK(final Integer topK) {
		this.topK = topK;
	}

	/**
	 * Returns the top-K parameter for the retrieval operation.
	 * @return the top-K value, can be null
	 */
	public Integer getTopK() {
		return this.topK;
	}

}