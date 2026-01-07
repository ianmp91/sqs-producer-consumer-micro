package com.example.sqsmicro.services;

import com.example.sqslib.iata.*;
import jakarta.xml.bind.JAXBElement;
import org.springframework.stereotype.Service;

import javax.xml.namespace.QName;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class FlightNotificationBuilder {

	// Namespace oficial definido en tu XML/XSD
	private static final String IATA_NS = "http://www.iata.org/IATA/2007/00";

	public IATAAIDXFlightLegNotifRQ buildNotif(String airlineCode, String flightNumber) {

		// 1. RAÍZ: IATAAIDXFlightLegNotifRQ
		var notif = new IATAAIDXFlightLegNotifRQ();

		// Headers de transporte IATA
		notif.setVersion(new BigDecimal("21.3"));
		notif.setTimeStamp(LocalDateTime.now());
		notif.setTarget("Test");
		notif.setTransactionIdentifier(UUID.randomUUID().toString());
		notif.setCorrelationID(UUID.randomUUID().toString()); // ID único para traza
		notif.setSequenceNmbr(BigInteger.ONE);
		notif.setTransactionStatusCode("Start");

		// 2. ORIGINATOR (Obligatorio en NotifRQ)
		// Indica quién está generando la alerta (Micro B)
		var originator = new IATAAIDXFlightLegNotifRQ.Originator();
		originator.setCompanyShortName("MicroService_B");
		originator.setCode("Airlines");
		originator.setCodeContext("Internal");
		notif.setOriginator(originator);

		// 3. FLIGHT LEG (El vuelo en sí)
		var flightLeg = new FlightLegType();

		// 3.1 Leg Identifier (Quién es el vuelo: LA 123 2025-01-01)
		// Nota: Asumo que tienes FlightLegIdentifierType generado
		var legId = new FlightLegIdentifierType();

		// Configura la aerolínea
		var airline = new FlightLegIdentifierType.Airline();
		airline.setValue(airlineCode); // Ej: LA
		airline.setCodeContext("IATA");
		legId.setAirline(airline);

		// Configura el número
		legId.setFlightNumber(flightNumber); // Ej: 1234

		// Configura la fecha (origen)
		legId.setOriginDate(createXmlDate(LocalDateTime.now()));

		// Configura origen/destino
		var depStation = new FlightLegIdentifierType.DepartureAirport();
		depStation.setValue("SCL");
		depStation.setCodeContext("IATA");
		legId.setDepartureAirport(depStation);

		var arrStation = new FlightLegIdentifierType.ArrivalAirport();
		arrStation.setValue("MIA");
		arrStation.setCodeContext("IATA");
		legId.setArrivalAirport(arrStation);

		flightLeg.setLegIdentifier(legId);

		// 3.2 Leg Data (El estado del vuelo: Programado, Aterrizado, etc)
		var legData = new FlightLegType.LegData();

		// Ejemplo: Agregar un estado operativo
		var opStatus = new OperationalStatusType();
		opStatus.setValue("SCH"); // Scheduled
		opStatus.setCodeContext("IATA");
		legData.getOperationalStatuses().add(opStatus);

		// Ejemplo JAXBElement (Campos opcionales con nillable/refs)
		// Service Type "J" (Passenger Scheduled)
		QName serviceTypeQName = new QName(IATA_NS, "ServiceType");
		JAXBElement<String> serviceTypeElement = new JAXBElement<>(serviceTypeQName, String.class, "J");
		legData.setServiceType(serviceTypeElement);

		flightLeg.setLegData(legData);

		// Agregar el leg a la lista de la notificación
		notif.getFlightLegs().add(flightLeg);

		return notif;
	}

	// Helper simple para XMLGregorianCalendar si no lo tienes
	private javax.xml.datatype.XMLGregorianCalendar createXmlDate(LocalDateTime ldt) {
		try {
			return javax.xml.datatype.DatatypeFactory.newInstance().newXMLGregorianCalendar(ldt.toString());
		} catch (Exception e) {
			return null;
		}
	}
}