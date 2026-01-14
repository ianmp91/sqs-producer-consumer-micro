package com.example.sqsmicro.records;

import java.util.Map;

/**
 * @author ian.paris
 * @since 2025-12-15
 */
public record MessageDto(
		Map<String, String> metadata, // Timestamp, TraceId, Sender, etc.
		String encryptedPayload,      // El contenido cifrado en Base64
		String encryptedKey, // Opcional: Para saber qué clave se usó
		String uniqueFlightId
) {}
