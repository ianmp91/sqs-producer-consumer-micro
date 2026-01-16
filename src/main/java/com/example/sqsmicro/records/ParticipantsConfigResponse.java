package com.example.sqsmicro.records;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * @author ian.paris
 * @since 2026-01-13
 */
public record ParticipantsConfigResponse(
		@JsonProperty("@odata.context") String odataContext,
		@JsonProperty("value") List<Participant> participants
) {
	public record Participant(
			@JsonProperty("Id") String id,
			@JsonProperty("Name") String name,
			@JsonProperty("IataCode") String iataCode,
			@JsonProperty("KafkaTopicStatus") boolean kafkaTopicStatus,
			@JsonProperty("PublicKey") String publicKey,
			@JsonProperty("PublicKeyStatus") String publicKeyStatus,
			@JsonProperty("RecipientType") String recipientType
	) {}
}