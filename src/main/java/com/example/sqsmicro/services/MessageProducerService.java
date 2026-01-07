package com.example.sqsmicro.services;

import com.example.sqslib.iata.IATAAIDXFlightLegNotifRQ;
import com.example.sqslib.iata.IATAAIDXFlightLegRQ;
import com.example.sqslib.service.XmlService;
import com.example.sqsmicro.records.MessageDto;
import com.example.sqslib.producer.SqsProducerService;
import com.example.sqsmicro.util.EncryptDecryptMessageUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
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
    private final String colaAwsSqsProducer;
    private final XmlService xmlService;
    private final FlightNotificationBuilder flightNotificationBuilder;


    public MessageProducerService(
            @Value("${cola.aws.sqs.producer}") String colaAwsSqsProducer,
            SqsProducerService sqsProducerService,
            EncryptDecryptMessageUtil encryptDecryptMessageUtil,
            XmlService xmlService,
            FlightNotificationBuilder flightNotificationBuilder) {
        this.colaAwsSqsProducer = colaAwsSqsProducer;
        this.sqsProducerService = sqsProducerService;
        this.encryptDecryptMessageUtil = encryptDecryptMessageUtil;
        this.xmlService = xmlService;
        this.flightNotificationBuilder = flightNotificationBuilder;
    }

    public void sendFlightLegNotifRequest() throws Exception {
        IATAAIDXFlightLegNotifRQ request = flightNotificationBuilder.buildNotif("QR", "1234");
        String xmlPayload = xmlService.toXml(request);
        log.debug("Before preparing the SQS shipment. Payload to encrypt: {}", xmlPayload);
        EncryptDecryptMessageUtil.EncryptedMessageBundle encryptedMessageBundle = encryptDecryptMessageUtil.encryptHybrid(xmlPayload);
        Map<String, String> requestMetadata = new HashMap<>();
        requestMetadata.put("message_type", "IATAAIDXFlightLegNotifRQ"); // Tipo de respuesta
        requestMetadata.put("correlation_id", "1234567890"); // Mantener trazabilidad
        requestMetadata.put("key_public", encryptDecryptMessageUtil.getPublicKeyAsString()); //
        log.debug("Before preparing the SQS shipment. Metadata: {}", requestMetadata);
        MessageDto message = new MessageDto(
                requestMetadata,
                encryptedMessageBundle.encryptedPayload(),
                encryptedMessageBundle.encryptedKey()
        );
        log.info("Preparing the SQS shipment.  EncryptedPayload: {}", encryptedMessageBundle.encryptedPayload());
        log.info("Preparing the SQS shipment.  EncryptedKey: {}", encryptedMessageBundle.encryptedKey());
        sqsProducerService.send(colaAwsSqsProducer, message);
    }

    public void sendFlightLegRequest() throws Exception {
        IATAAIDXFlightLegRQ request = new IATAAIDXFlightLegRQ();
        request.setEchoToken(UUID.randomUUID().toString());
        request.setTimeStamp(LocalDateTime.now());
        request.setTarget("test");
        request.setVersion(new BigDecimal("21.3"));
        request.setTransactionIdentifier("t1");
        request.setSequenceNmbr(new BigInteger("1"));
        request.setCorrelationID("t1");
        request.setTransactionStatusCode("Start");
        request.setRetransmissionIndicator(false);
        IATAAIDXFlightLegRQ.Airline airline = new IATAAIDXFlightLegRQ.Airline();
        airline.setCode("QR");
        airline.setCodeContext("1234");
        request.setAirline(airline);
        String xmlPayload = xmlService.toXml(request);
        log.debug("Before preparing the SQS shipment. Payload to encrypt: {}", xmlPayload);
        EncryptDecryptMessageUtil.EncryptedMessageBundle encryptedMessageBundle = encryptDecryptMessageUtil.encryptHybrid(xmlPayload);
        Map<String, String> requestMetadata = new HashMap<>();
        requestMetadata.put("message_type", "IATAAIDXFlightLegRQ"); // Tipo de respuesta
        requestMetadata.put("correlation_id", "1234567890"); // Mantener trazabilidad
        requestMetadata.put("key_public", encryptDecryptMessageUtil.getPublicKeyAsString()); //
        log.debug("Before preparing the SQS shipment. Metadata: {}", requestMetadata);
        MessageDto message = new MessageDto(
                requestMetadata,
                encryptedMessageBundle.encryptedPayload(),
                encryptedMessageBundle.encryptedKey()
        );
        log.info("Preparing the SQS shipment.  EncryptedPayload: {}", encryptedMessageBundle.encryptedPayload());
        log.info("Preparing the SQS shipment.  EncryptedKey: {}", encryptedMessageBundle.encryptedKey());
        sqsProducerService.send(colaAwsSqsProducer, message);
    }

    public void sendMessage(String payload, Map<String, String> metadata) throws Exception {
        log.debug("Before preparing the SQS shipment. Payload to encrypt: {}", payload);
        EncryptDecryptMessageUtil.EncryptedMessageBundle encryptedMessageBundle = encryptDecryptMessageUtil.encryptHybrid(payload);
        metadata.put("key_public", encryptDecryptMessageUtil.getPublicKeyAsString());
        MessageDto message = new MessageDto(
                metadata,
                encryptedMessageBundle.encryptedPayload(),
                encryptedMessageBundle.encryptedKey()
        );
        log.info("Preparing the SQS shipment. Metadata: {} | EncryptedPayload: {}", metadata.toString(), encryptedMessageBundle.encryptedPayload());
        sqsProducerService.send(colaAwsSqsProducer, message);
    }
}