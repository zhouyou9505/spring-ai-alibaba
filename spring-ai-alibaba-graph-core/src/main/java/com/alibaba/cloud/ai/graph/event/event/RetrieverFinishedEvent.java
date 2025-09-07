package com.alibaba.cloud.ai.graph.event.event;

import com.alibaba.cloud.ai.graph.event.type.EventType;

/**
 * An event that represents the completion of a retriever operation.
 * <p>
 * This event is fired when a retriever finishes its operation lifecycle, either
 * successfully or after completing the retrieval process. It captures the final
 * state and results of the retriever's execution to enable monitoring, debugging,
 * and result processing.
 * </p>
 * <p>
 * The event automatically sets its type to {@link EventType#RETRIEVER_END} and provides
 * fields to store completion metadata and execution results.
 * </p>
 *
 * @see com.alibaba.cloud.ai.graph.event.event.BaseEvent
 * @see EventType#RETRIEVER_END
 * @see RetrieverStartEvent
 * @see RetrieverErrorEvent
 * @author System Generated
 */
public class RetrieverFinishedEvent extends BaseEvent {

	private String retrieverId;

	private String retrieverName;

	private String query;

	private Integer documentsFound;

	private Object documents;

	/**
	 * Creates a new RetrieverEndEvent with type set to {@link EventType#RETRIEVER_END}.
	 * <p>
	 * The timestamp is automatically set to the current time and all fields
	 * are initialized as null.
	 * </p>
	 */
	public RetrieverFinishedEvent() {
		super(EventType.RETRIEVER_END);
	}

	/**
	 * Sets the unique identifier of the retriever that has finished.
	 * @param retrieverId the retriever identifier. Can be null.
	 */
	public void setRetrieverId(final String retrieverId) {
		this.retrieverId = retrieverId;
	}

	/**
	 * Returns the unique identifier of the retriever that has finished.
	 * @return the retriever identifier, can be null
	 */
	public String getRetrieverId() {
		return this.retrieverId;
	}

	/**
	 * Sets the name of the retriever that has finished.
	 * @param retrieverName the retriever name. Can be null.
	 */
	public void setRetrieverName(final String retrieverName) {
		this.retrieverName = retrieverName;
	}

	/**
	 * Returns the name of the retriever that has finished.
	 * @return the retriever name, can be null
	 */
	public String getRetrieverName() {
		return this.retrieverName;
	}

	/**
	 * Sets the query text that was used for retrieval.
	 * @param query the query text. Can be null.
	 */
	public void setQuery(final String query) {
		this.query = query;
	}

	/**
	 * Returns the query text that was used for retrieval.
	 * @return the query text, can be null
	 */
	public String getQuery() {
		return this.query;
	}

	/**
	 * Sets the number of documents found by the retriever.
	 * @param documentsFound the number of documents found. Can be null.
	 */
	public void setDocumentsFound(final Integer documentsFound) {
		this.documentsFound = documentsFound;
	}

	/**
	 * Returns the number of documents found by the retriever.
	 * @return the number of documents found, can be null
	 */
	public Integer getDocumentsFound() {
		return this.documentsFound;
	}

	/**
	 * Sets the documents retrieved by the retriever operation.
	 * @param documents the retrieved documents. Can be null.
	 */
	public void setDocuments(final Object documents) {
		this.documents = documents;
	}

	/**
	 * Returns the documents retrieved by the retriever operation.
	 * @return the retrieved documents, can be null
	 */
	public Object getDocuments() {
		return this.documents;
	}

}