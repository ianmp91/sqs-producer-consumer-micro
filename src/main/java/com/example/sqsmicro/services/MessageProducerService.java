package com.example.sqsmicro.services;

import com.example.sqslib.iata.IATAAIDXFlightLegNotifRQ;
import com.example.sqslib.iata.IATAAIDXFlightLegRQ;
import com.example.sqslib.service.XmlService;
import com.example.sqsmicro.builders.FlightNotificationBuilder;
import com.example.sqsmicro.records.MessageDto;
import com.example.sqslib.producer.SqsProducerService;
import com.example.sqsmicro.records.UniqueFlightId;
import com.example.sqsmicro.util.EncryptDecryptMessageUtil;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import lombok.extern.slf4j.Slf4j;

/**
 * @author ian.paris
 * @since 2025-12-15
 */
@Slf4j
@Service
public class MessageProducerService {

    private final SqsProducerService sqsProducerService;
    private final EncryptDecryptMessageUtil encryptDecryptMessageUtil;
    private final XmlService xmlService;
    private final FlightNotificationBuilder flightNotificationBuilder;
    private final ConfigurationLoaderService configurationLoaderService;


    public MessageProducerService(
            SqsProducerService sqsProducerService,
            EncryptDecryptMessageUtil encryptDecryptMessageUtil,
            XmlService xmlService,
            FlightNotificationBuilder flightNotificationBuilder,
            ConfigurationLoaderService configurationLoaderService) {
        this.sqsProducerService = sqsProducerService;
        this.encryptDecryptMessageUtil = encryptDecryptMessageUtil;
        this.xmlService = xmlService;
        this.flightNotificationBuilder = flightNotificationBuilder;
        this.configurationLoaderService = configurationLoaderService;
    }

    public void sendFlightLegNotifRequest() throws Exception {
        String target = "airport-c";
        String targetQueue = configurationLoaderService.getPeerTargetQueue(target);
        String receiverPubKey = configurationLoaderService.getPeerPublicKey(target);
        log.debug("Before preparing the SQS shipment. TargetQueue {} | ReceiverPubKey {}", targetQueue, receiverPubKey);
        IATAAIDXFlightLegNotifRQ request = flightNotificationBuilder.buildNotif("QR", "1234");
        String xmlPayload = xmlService.toXml(request);
        UniqueFlightId uniqueFlightId = new UniqueFlightId(request.getFlightLegs().getFirst().getLegIdentifier().getAirline().getValue(), request.getFlightLegs().getFirst().getLegIdentifier().getFlightNumber(), request.getTimeStamp().toLocalDate(), request.getFlightLegs().getFirst().getLegIdentifier().getDepartureAirport().getValue(), request.getFlightLegs().getFirst().getLegIdentifier().getArrivalAirport().getValue(), Optional.empty(), Optional.empty());
        log.debug("Before preparing the SQS shipment. Payload to encrypt: {}", xmlPayload);
        encryptDecryptMessageUtil.loadPublicKey(receiverPubKey);
        EncryptDecryptMessageUtil.EncryptedMessageBundle encryptedMessageBundle = encryptDecryptMessageUtil.encryptHybrid(xmlPayload);
        Map<String, String> requestMetadata = new HashMap<>();
        requestMetadata.put("message_type", "IATAAIDXFlightLegNotifRQ");
        requestMetadata.put("correlation_id", request.getCorrelationID());
        requestMetadata.put("key_public", encryptDecryptMessageUtil.getPublicKeyAsString());
        log.debug("Before preparing the SQS shipment. Metadata: {}", requestMetadata);
        MessageDto message = new MessageDto(
                requestMetadata,
                encryptedMessageBundle.encryptedPayload(),
                encryptedMessageBundle.encryptedKey(),
                uniqueFlightId.toKeyId()
        );
        log.info("Preparing the SQS shipment. Metadata: {} | EncryptedPayload: {}", requestMetadata.toString(), encryptedMessageBundle.encryptedPayload());
        log.info("Preparing the SQS shipment. EncryptedKey: {}", message.encryptedKey());
        log.info("Preparing the SQS shipment. UniqueFlightId: {}", message.uniqueFlightId());
        sqsProducerService.send(targetQueue, message);
    }

    public void sendFlightLegRequest() throws Exception {
        String target = "airport-c";
        String targetQueue = configurationLoaderService.getPeerTargetQueue(target);
        String receiverPubKey = configurationLoaderService.getPeerPublicKey(target);
        log.debug("Before preparing the SQS shipment. TargetQueue {} | ReceiverPubKey {}", targetQueue, receiverPubKey);
        IATAAIDXFlightLegRQ request = new IATAAIDXFlightLegRQ();
        request.setEchoToken(UUID.randomUUID().toString());
        request.setTimeStamp(LocalDateTime.now());
        request.setTarget("Test");
        request.setVersion(new BigDecimal("21.3"));
        request.setTransactionIdentifier("t2");
        request.setSequenceNmbr(BigInteger.TWO);
        request.setCorrelationID("t2");
        request.setTransactionStatusCode("Start");
        request.setRetransmissionIndicator(false);
        IATAAIDXFlightLegRQ.Airline airline = new IATAAIDXFlightLegRQ.Airline();
        airline.setCode("QR");
        airline.setCodeContext("IATA");
        request.setAirline(airline);
        String xmlPayload = xmlService.toXml(request);
        log.debug("Before preparing the SQS shipment. Payload to encrypt: {}", xmlPayload);
        encryptDecryptMessageUtil.loadPublicKey(receiverPubKey);
        EncryptDecryptMessageUtil.EncryptedMessageBundle encryptedMessageBundle = encryptDecryptMessageUtil.encryptHybrid(xmlPayload);
        Map<String, String> requestMetadata = new HashMap<>();
        requestMetadata.put("message_type", "IATAAIDXFlightLegRQ");
        requestMetadata.put("correlation_id", request.getCorrelationID());
        requestMetadata.put("key_public", encryptDecryptMessageUtil.getPublicKeyAsString());
        UniqueFlightId uniqueFlightId = new UniqueFlightId(request.getAirline().getCode(), "", request.getTimeStamp().toLocalDate(), "*", "*", Optional.empty(), Optional.empty());
        log.debug("Before preparing the SQS shipment. Metadata: {}", requestMetadata);
        MessageDto message = new MessageDto(
                requestMetadata,
                encryptedMessageBundle.encryptedPayload(),
                encryptedMessageBundle.encryptedKey(),
                uniqueFlightId.toKeyId()
        );
        log.info("Preparing the SQS shipment. Metadata: {} | EncryptedPayload: {}", requestMetadata.toString(), encryptedMessageBundle.encryptedPayload());
        log.info("Preparing the SQS shipment. EncryptedKey: {}", message.encryptedKey());
        log.info("Preparing the SQS shipment. UniqueFlightId: {}", message.uniqueFlightId());
        sqsProducerService.send(targetQueue, message);
    }

    public void sendMessage(String payload, Map<String, String> metadata) throws Exception {
        String target = "airport-c";
        String targetQueue = configurationLoaderService.getPeerTargetQueue(target);
        String receiverPubKey = configurationLoaderService.getPeerPublicKey(target);
        log.debug("Before preparing the SQS shipment. TargetQueue {} | ReceiverPubKey {}", targetQueue, receiverPubKey);
        log.debug("Before preparing the SQS shipment. Payload to encrypt: {}", payload);
        encryptDecryptMessageUtil.loadPublicKey(receiverPubKey);
        EncryptDecryptMessageUtil.EncryptedMessageBundle encryptedMessageBundle = encryptDecryptMessageUtil.encryptHybrid(payload);
        metadata.put("message_type", metadata.get("message_type"));
        metadata.put("correlation_id", metadata.get("correlation_id"));
        metadata.put("key_public", encryptDecryptMessageUtil.getPublicKeyAsString());
        UniqueFlightId uniqueFlightId = new UniqueFlightId("QR", "1234", LocalDate.now(), "LAX", "GRU", Optional.empty(), Optional.empty());
        MessageDto message = new MessageDto(
                metadata,
                encryptedMessageBundle.encryptedPayload(),
                encryptedMessageBundle.encryptedKey(),
                uniqueFlightId.toKeyId()
        );
        log.info("Preparing the SQS shipment. Metadata: {} | EncryptedPayload: {}", metadata.toString(), encryptedMessageBundle.encryptedPayload());
        log.info("Preparing the SQS shipment. EncryptedKey: {}", message.encryptedKey());
        log.info("Preparing the SQS shipment. UniqueFlightId: {}", message.uniqueFlightId());
        sqsProducerService.send(targetQueue, message);
    }
}