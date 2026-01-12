package com.example.sqsmicro.services;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author ian.paris
 * @since 2025-12-18
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SchedulerService {
	private final MessageProducerService messageProducerService;
	private List<String> loadedMessages;
	private final AtomicInteger counter = new AtomicInteger(0);

	@Value("classpath:payloads.txt")
	private Resource payloadsResource;

	@PostConstruct
	public void init() {
		try (var reader = new BufferedReader(
				new InputStreamReader(payloadsResource.getInputStream(), StandardCharsets.UTF_8))) {

			loadedMessages = reader.lines().toList(); // Java Stream API
			log.debug("Successful upload: {} messages ready to be sent.", loadedMessages.size());

		} catch (Exception e) {
			log.error("Fatal error loading payloads.txt", e);
		}
	}

	@Scheduled(cron = "${scheduler.cron-expression:0 */2 * * * *}")
	public void sendAutomatedMessage() {
		if (loadedMessages == null || loadedMessages.isEmpty()) {
			log.warn("There are no messages to send...");
			return;
		}

		try {
			// 1. Obtain the message using Round Robin (0, 1, 2... 9, 0, 1...)
			int index = counter.getAndIncrement() % loadedMessages.size();
			String rawPayload = loadedMessages.get(index);

			// 2. Create dynamic metadata
			Map<String, String> metadata = new HashMap<>();

			log.info(">>> CRON FIRED: Sending message # {}", index);

			// 3. Reuse your existing service (which ENCRYPT and SEND)
			messageProducerService.sendMessage(rawPayload, metadata);

		} catch (Exception e) {
			log.error("Error in cron execution", e);
		}
	}

}

