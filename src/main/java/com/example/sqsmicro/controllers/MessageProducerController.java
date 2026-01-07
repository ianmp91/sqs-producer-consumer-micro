package com.example.sqsmicro.controllers;

import com.example.sqsmicro.services.MessageProducerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * @author ian.paris
 * @since 2025-12-15
 */
@RestController
@RequestMapping("/api/v1/producer")
@RequiredArgsConstructor
@Tag(name = "Trigger SQS", description = "Endpoint to initiate the encrypted messaging flow")
public class MessageProducerController {

    private final MessageProducerService messageProducerService;

    @PostMapping("/send")
    @Operation(
            summary = "Send encrypted message to SQS-1",
            description = "It receives a payload and metadata, encrypts the payload with a public key, and sends it to the aws-sqs-1 queue."
    )
    @ApiResponse(responseCode = "200", description = "Message successfully sent to queue")
    public String handleSendMessage(@RequestBody MessagePayloadDto messagePayloadDto) throws Exception {
        String payload = messagePayloadDto.getPayload();
        Map<String, String> metadata = messagePayloadDto.getMetadata();
        messageProducerService.sendMessage(payload, metadata);
        return "Encrypted message sent to SQS Queue 1.";
    }

    @PostMapping("/send/flightleg")
    @Operation(
            summary = "Send encrypted message to SQS-1",
            description = "Create & encrypts the payload with a public key, and sends it to the aws-sqs-1 queue."
    )
    @ApiResponse(responseCode = "200", description = "Message successfully sent to queue")
    public String handleSendFlightLegRequestMessage() throws Exception {
        messageProducerService.sendFlightLegRequest();
        return "Encrypted message sent to SQS Queue 1.";
    }

    @PostMapping("/send/notification")
    @Operation(
            summary = "Send encrypted message to SQS-1",
            description = "Create & encrypts the payload with a public key, and sends it to the aws-sqs-1 queue."
    )
    @ApiResponse(responseCode = "200", description = "Message successfully sent to queue")
    public String handleSendFlightLegNotifRequestMessage() throws Exception {
        messageProducerService.sendFlightLegNotifRequest();
        return "Encrypted message sent to SQS Queue 1.";
    }
}
