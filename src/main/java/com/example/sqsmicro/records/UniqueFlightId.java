package com.example.sqsmicro.records;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

/**
 * @author ian.paris
 * @since 2026-01-11
 */
public record UniqueFlightId(
		String airline,             // E2.1: IATA/ICAO code (e.g., "UA")
		String flightNumber,        // E2.2: Numeric part (e.g., "096")
		LocalDate originDate,       // E2.4: Scheduled Departure Date en UTC (vital para multi-leg)
		String departureAirport,    // E2.5: Origin Airport Code (e.g., "SFO")
		String arrivalAirport,      // E2.6: Originally Scheduled Arrival Airport (e.g., "LHR")
		Optional<String> suffix,    // E2.3: Operational Suffix (e.g., "Z"). Optional.
		Optional<Integer> repeatNumber // E2.7: Repeat Number (para intentos fallidos/retornos). Optional.
) {

	// Formateador estático para la fecha en el ID (ISO 8601 básico es seguro para IDs: YYYYMMDD)
	private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.BASIC_ISO_DATE;

	/**
	 * Compact Constructor para validación y normalización según AIDX.
	 */
	public UniqueFlightId {
		if (airline == null || airline.isBlank()) {
			throw new IllegalArgumentException("Airline code cannot be null or empty");
		}
		if (originDate == null) {
			throw new IllegalArgumentException("OriginDate (UTC) cannot be null");
		}
		if (departureAirport == null || departureAirport.isBlank()) {
			throw new IllegalArgumentException("Departure Airport cannot be null or empty");
		}
		if (arrivalAirport == null || arrivalAirport.isBlank()) {
			throw new IllegalArgumentException("Arrival Airport cannot be null or empty");
		}

		// AIDX E2.2: "Flight numbers of two digits or less are padded with leading zeros"
		// Normalizamos a 4 dígitos para consistencia interna (estándar de facto en sistemas modernos),
		// aunque AIDX menciona padding a 3. Ajusta a "%03d" si prefieres estricto AIDX legado.
		// Se asume que flightNumber viene numérico o parseable.
		flightNumber = normalizeFlightNumber(flightNumber);

		// Normalización de Aeropuertos a mayúsculas
		airline = airline.toUpperCase();
		departureAirport = departureAirport.toUpperCase();
		arrivalAirport = arrivalAirport.toUpperCase();
	}

	/**
	 * Genera el ID único en formato String para usar como 'encryptedKey' en MessageDto
	 * o 'MessageGroupId' en AWS SQS FIFO.
	 * Formato: AIRLINE-FLIGHTNUM-DATE-DEP-ARR[-SUFFIX][-REPEAT]
	 */
	public String toKeyId() {
		StringBuilder sb = new StringBuilder();
		sb.append(airline).append("-")
				.append(flightNumber).append("-")
				.append(originDate.format(DATE_FORMATTER)).append("-")
				.append(departureAirport).append("-")
				.append(arrivalAirport);

		suffix.ifPresent(s -> sb.append("-").append(s.toUpperCase()));
		repeatNumber.ifPresent(r -> sb.append("-").append(r));

		return sb.toString();
	}

	private String normalizeFlightNumber(String rawNumber) {
		if (rawNumber == null || rawNumber.isBlank()) return "0000";
		try {
			// Intenta parsear a número y hacer padding a 4 dígitos (ej: "96" -> "0096")
			int num = Integer.parseInt(rawNumber.replaceAll("[^0-9]", ""));
			return String.format("%04d", num);
		} catch (NumberFormatException e) {
			// Si es alfanumérico complejo (raro en AIDX puro pero posible en legacy), lo devolvemos tal cual
			return rawNumber;
		}
	}
}
