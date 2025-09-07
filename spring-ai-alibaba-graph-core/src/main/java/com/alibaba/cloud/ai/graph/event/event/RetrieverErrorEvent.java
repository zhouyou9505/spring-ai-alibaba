package com.alibaba.cloud.ai.graph.event.event;

import com.alibaba.cloud.ai.graph.event.type.EventType;

/**
 * An event that represents an error that occurred during a retriever operation.
 * <p>
 * This event is fired when an error occurs during the execution of a retriever
 * operation. It captures the error information to enable proper error handling and
 * reporting throughout the application.
 * </p>
 * <p>
 * The event automatically sets its type to {@link EventType#RETRIEVER_ERROR} and provides
 * fields to store error information and retriever context.
 * </p>
 *
 * @see com.alibaba.cloud.ai.graph.event.event.BaseEvent
 * @see EventType#RETRIEVER_ERROR
 * @see RetrieverStartEvent
 * @see RetrieverFinishedEvent
 * @author System Generated
 */
public class RetrieverErrorEvent extends BaseEvent {

	private String retrieverId;

	private String retrieverName;

	private String query;

	private String error;

	/**
	 * Creates a new RetrieverErrorEvent with type set to {@link EventType#RETRIEVER_ERROR}.
	 * <p>
	 * The timestamp is automatically set to the current time and all fields
	 * are initialized as null.
	 * </p>
	 */
	public RetrieverErrorEvent() {
		super(EventType.RETRIEVER_ERROR);
	}

	/**
	 * Sets the unique identifier of the retriever that encountered an error.
	 * @param retrieverId the retriever identifier. Can be null.
	 */
	public void setRetrieverId(final String retrieverId) {
		this.retrieverId = retrieverId;
	}

	/**
	 * Returns the unique identifier of the retriever that encountered an error.
	 * @return the retriever identifier, can be null
	 */
	public String getRetrieverId() {
		return this.retrieverId;
	}

	/**
	 * Sets the name of the retriever that encountered an error.
	 * @param retrieverName the retriever name. Can be null.
	 */
	public void setRetrieverName(final String retrieverName) {
		this.retrieverName = retrieverName;
	}

	/**
	 * Returns the name of the retriever that encountered an error.
	 * @return the retriever name, can be null
	 */
	public String getRetrieverName() {
		return this.retrieverName;
	}

	/**
	 * Sets the query text that was being processed when the error occurred.
	 * @param query the query text. Can be null.
	 */
	public void setQuery(final String query) {
		this.query = query;
	}

	/**
	 * Returns the query text that was being processed when the error occurred.
	 * @return the query text, can be null
	 */
	public String getQuery() {
		return this.query;
	}

	/**
	 * Sets the error message or description for this event.
	 * @param error the error message or description. Can be null.
	 */
	public void setError(final String error) {
		this.error = error;
	}

	/**
	 * Returns the error message or description associated with this event.
	 * @return the error message, can be null
	 */
	public String getError() {
		return this.error;
	}

}