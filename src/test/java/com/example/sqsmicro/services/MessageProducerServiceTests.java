package com.example.sqsmicro.services;

import com.example.sqslib.producer.SqsProducerService;
import com.example.sqslib.service.XmlService;
import com.example.sqsmicro.builders.FlightNotificationBuilder;
import com.example.sqsmicro.records.MessageDto;
import com.example.sqsmicro.util.EncryptDecryptMessageUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author ian.paris
 * @since 2025-12-15
 */
@ExtendWith(MockitoExtension.class)
public class MessageProducerServiceTests {

    @Mock
    private ExternalConfigService externalConfigService;

    @Mock
    private SqsProducerService sqsProducerLib; // Mock de tu Libreria (A)

    @Mock
    private EncryptDecryptMessageUtil encryptDecryptMessageUtil;

    @Mock
    private FlightNotificationBuilder flightNotificationBuilder;

    @Mock
        private XmlService xmlService; // Mock de encriptación

    private MessageProducerService messageProducerService; // Servicio (B) bajo test

    @Test
    void testSendEncryptedRequest_ShouldEncryptAndSendToQueue1() throws Exception {
        // GIVEN
        String rawPayload = "LAX 123";
        Map<String,String> metadata = new HashMap<>();
        String publicKey = "key-public-123";
        metadata.put("publicKey", publicKey);
        when(externalConfigService.getQueueUrl()).thenReturn("cola-aws-sqs-1");
        // 1. Stub to encrypt the payload (your service now returns String)
        when(encryptDecryptMessageUtil.encryptHybrid(eq(rawPayload)))
                .thenReturn(new EncryptDecryptMessageUtil.EncryptedMessageBundle(rawPayload, publicKey));
        // The safest and cleanest way is to instantiate it:
        messageProducerService = new MessageProducerService(
                sqsProducerLib,
                encryptDecryptMessageUtil,
                xmlService,
                flightNotificationBuilder,
                externalConfigService
        );
        // WHEN
        messageProducerService.sendMessage(rawPayload, metadata);
        // THEN
        verify(encryptDecryptMessageUtil).encryptHybrid(rawPayload);
        // We verified that the sender was called with the transformed data
        verify(sqsProducerLib).send(eq("cola-aws-sqs-1"), any(MessageDto.class));
    }
}
