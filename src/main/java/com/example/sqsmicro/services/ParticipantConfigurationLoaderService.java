package com.example.sqsmicro.services;

import com.example.sqsmicro.records.ParticipantsConfigResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author ian.paris
 * @since 2026-01-13
 */
@Service("participantConfigurationLoaderService")
public class ParticipantConfigurationLoaderService {

	private final RestClient restClient;

	// Identidad propia
	private static final String MY_SERVICE_ID = "QR";

	// 1. MEJORA: El caché ahora guarda directamente el objeto Participant.
	// Key = IataCode (ej: 'QR'), Value = El Record Participant
	private final Map<String, ParticipantsConfigResponse.Participant> servicesConfigurationCache = new ConcurrentHashMap<>();

	// Set de servicios que queremos mantener actualizados automáticamente
	private final Set<String> trackedServices = ConcurrentHashMap.newKeySet();

	public ParticipantConfigurationLoaderService(@Qualifier("configRestClient2") RestClient restClient) {
		this.restClient = restClient;
		this.trackedServices.add(MY_SERVICE_ID);
	}

	/**
	 * Carga/Refresca la configuración de un servicio específico.
	 */
	public ParticipantsConfigResponse.Participant loadConfigurationFor(String serviceName) {
		try {
			System.out.println("Config actualizada serivceName: " + serviceName);
			var response = restClient.get()
					// Asegúrate de que '{serviceName}' se reemplace correctamente.
					// RestClient usa uri variables, así que el placeholder debe ser {serviceName} en el string, no '{serviceName}'
					.uri("/Participants?%24filter=IataCode%20eq%20'{serviceName}'", serviceName)
					.retrieve()
					.body(ParticipantsConfigResponse.class);

			System.out.println("Config actualizada response.participants().size: " + response.participants().size());

			if (response != null && response.participants() != null && !response.participants().isEmpty()) {

				// 2. MEJORA: Como filtras por IataCode, tomas el primero que coincida.
				var participantConfig = response.participants().stream()
								.filter(p -> p.iataCode().equals(serviceName))
								.findFirst()
								.orElse(null);
				System.out.println("Config actualizada participantConfig: " + participantConfig);

				servicesConfigurationCache.put(serviceName, participantConfig);

				if (!trackedServices.contains(serviceName) && !serviceName.equals(MY_SERVICE_ID)) {
					trackedServices.add(serviceName);
				}

				System.out.println("Config actualizada para: " + serviceName);

				return participantConfig;
			}
		} catch (Exception e) {
			System.err.println("Error fetching participants config: " + e.getMessage());
		}
		return null;
	}

	//@Scheduled(fixedRate = 600000) // 10 minutos
	public void refreshAllTrackedConfigurations() {
		trackedServices.forEach(this::loadConfigurationFor);
	}

	// ==========================================
	// MÉTODOS DE ACCESO A PROPIEDADES
	// ==========================================

	/**
	 * Obtiene el objeto Participant completo (Type-safe)
	 */
	public ParticipantsConfigResponse.Participant getParticipant(String serviceName) {
		if (!servicesConfigurationCache.containsKey(serviceName)) {
			loadConfigurationFor(serviceName);
		}
		return servicesConfigurationCache.get(serviceName);
	}

	/**
	 * 3. SOLUCIÓN FINAL: Acceso directo a la PublicKey usando el getter del Record
	 */
	public String getPeerPublicKey(String targetServiceName) {
		var participant = getParticipant(targetServiceName);

		if (participant != null) {
			// Aquí es donde lees la key correctamente usando el método del record
			return participant.publicKey();
		}

		return null; // O lanzar excepción si es crítico
	}

	public String getPeerTargetName(String targetServiceName) {
		var participant = getParticipant(targetServiceName);

		if (participant != null) {
			// Aquí es donde lees la key correctamente usando el método del record
			return participant.name();
		}

		return null;
	}

	public String getPeerTargetQueue(String targetServiceName) {
		var participant = getParticipant(targetServiceName);

		if (participant != null) {
			// Aquí es donde lees la key correctamente usando el método del record
			return participant.name();
		}

		return null;
	}
}