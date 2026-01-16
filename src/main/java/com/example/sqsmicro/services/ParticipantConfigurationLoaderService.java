package com.example.sqsmicro.services;

import com.example.sqsmicro.records.ParticipantsConfigResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * @author ian.paris
 * @since 2026-01-13
 */
@Service("participantConfigurationLoaderService")
public class ParticipantConfigurationLoaderService {

	private final RestClient restClient;

	// Identidad propia
	private static final String MY_SERVICE_ID = "LAX";

	// Cache segregada: Key = Nombre del Servicio (ej: 'airport-c'), Value = Sus propiedades
	private final Map<String, Map<String, Object>> servicesConfigurationCache = new ConcurrentHashMap<>();

	// Set de servicios que queremos mantener actualizados automáticamente con el @Scheduled
	private final Set<String> trackedServices = ConcurrentHashMap.newKeySet();

	public ParticipantConfigurationLoaderService(@Qualifier("configRestClient2") RestClient restClient) {
		this.restClient = restClient;
		// Al iniciar, siempre queremos rastrear nuestra propia config
		this.trackedServices.add(MY_SERVICE_ID);
	}

	/**
	 * Carga/Refresca la configuración de un servicio específico.
	 * Retorna el mapa de propiedades para uso inmediato si se desea.
	 */
	public Map<String, Object> loadConfigurationFor(String serviceName) {
		try {
			// 1. Ajustar la llamada al endpoint (El path debe coincidir con tu API OData)
			var response = restClient.get()
					.uri("/Participants?%24filter=IataCode%20eq%20'{serviceName}'", serviceName) // <-- Ajustar esto según tu ruta real en Azure/API
					.retrieve()
					.body(ParticipantsConfigResponse.class);

			// 2. Validar la respuesta basada en la nueva estructura
			if (response != null && response.participants() != null) {

				// 3. Transformar la Lista en un Mapa
				// Clave: IataCode (ej: "QR"), Valor: El objeto Participant completo
				Map<String, Object> newProperties = response.participants().stream()
						.collect(Collectors.toMap(
								ParticipantsConfigResponse.Participant::iataCode, // Key
								participant -> participant                        // Value
						));

				// Guardamos en caché.
				// Ahora 'newProperties' es un mapa donde config.get("QR") te devuelve el objeto de Qatar Airways
				servicesConfigurationCache.put(serviceName, newProperties);

				if (!trackedServices.contains(serviceName)) {
					trackedServices.add(serviceName);
				}

				System.out.println("Participants config updated. Count: " + newProperties.size());
				return newProperties;
			}
		} catch (Exception e) {
			System.err.println("Error fetching participants config: " + e.getMessage());
		}
		return Map.of();
	}

	//@Scheduled(fixedRate = 600000) // 10 minutos
	public void refreshAllTrackedConfigurations() {
		// Refresca la config propia y la de cualquier peer que hayamos consultado antes
		trackedServices.forEach(this::loadConfigurationFor);
	}

	// ==========================================
	// MÉTODOS DE ACCESO A PROPIEDADES
	// ==========================================

	private Object getProperty(String serviceName, String key) {
		// Si no tenemos la config de ese servicio, intentamos cargarla por primera vez (Lazy Loading)
		if (!servicesConfigurationCache.containsKey(serviceName)) {
			loadConfigurationFor(serviceName);
		}

		var props = servicesConfigurationCache.get(serviceName);
		return (props != null) ? props.get(key) : null;
	}

	// --- MI CONFIGURACIÓN (Airlines-B) ---

	public String getMyPrivateKey() {
		// Buscamos en 'airlines-b', la propiedad standard
		return (String) getProperty(MY_SERVICE_ID, "config.security.private-key");
	}

	public String getMyListeningQueue() {
		// Buscamos en 'airlines-b', mi inbound
		return (String) getProperty(MY_SERVICE_ID, "config.queues.inbound");
	}

	// --- CONFIGURACIÓN DE TERCEROS (Peers) ---

	/**
	 * Obtiene la clave pública de un servicio destino (ej: airport-c)
	 * Lee DIRECTAMENTE el archivo de config de 'airport-c'
	 */
	public String getPeerPublicKey(String targetServiceName) {
		// Nota: Buscamos "security.public-key" dentro de la config del target
		return (String) getProperty(targetServiceName, "config.security.public-key");
	}

	/**
	 * Obtiene la cola donde el servicio destino ESPERA recibir mensajes.
	 * Lee DIRECTAMENTE el archivo de config de 'airport-c' propiedad 'inbound'
	 */
	public String getPeerTargetQueue(String targetServiceName) {
		// Para enviarle a C, necesito saber cuál es SU inbound
		return (String) getProperty(targetServiceName, "config.queues.inbound");
	}
}