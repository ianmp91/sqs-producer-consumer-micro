package com.example.sqsmicro.listener;

import com.example.sqsmicro.records.MessageDto;
import com.example.sqsmicro.util.EncryptDecryptMessageUtil;
import io.awspring.cloud.sqs.annotation.SqsListener;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

/**
 * @author ian.paris
 * @since 2025-12-15
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SqsListenerConsumer {

    private final EncryptDecryptMessageUtil encryptDecryptMessageUtil;

    @SqsListener(queueNames = "#{@externalConfigService.getListeningQueue()}")
    public void listenResponse(@Payload MessageDto messageDto) throws Exception {
        log.debug("Message encryptedKey received from cola-aws-sqs-2: " + messageDto.encryptedKey());
        log.debug("Message metadata received from cola-aws-sqs-2: " + messageDto.metadata());
        log.debug("Message uniqueFlightId received from cola-aws-sqs-2: " + messageDto.uniqueFlightId());
        String messagePayload = encryptDecryptMessageUtil.decryptHybrid(messageDto.encryptedPayload(), messageDto.encryptedKey());
        log.debug("Message received from cola-aws-sqs-2: {}", messagePayload);
    }

}
