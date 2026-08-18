package com.airport.model;

import com.airport.model.enums.PassengerClass;
import java.util.Objects;

/**
 * Yolcu modeli.
 * Bagaj ile ilişkisi: bir yolcunun birden fazla bagajı olabilir (1-N).
 * BaggageTrackingService passengerBaggageIndex ile yolcuya ait tüm bagajları tutar.
 */
public class Passenger {

    private final String passengerId;
    private String name;
    private String surname;
    private PassengerClass passengerClass;
    private String flightNumber;
    private String nationalId; // TC Kimlik / Pasaport No

    // ==================== CONSTRUCTOR ====================

    public Passenger(String passengerId, String name, String surname,
                     PassengerClass passengerClass, String flightNumber) {
        this.passengerId = passengerId;
        this.name = name;
        this.surname = surname;
        this.passengerClass = passengerClass;
        this.flightNumber = flightNumber;
    }

    public Passenger(String passengerId, String name, String surname,
                     PassengerClass passengerClass, String flightNumber, String nationalId) {
        this(passengerId, name, surname, passengerClass, flightNumber);
        this.nationalId = nationalId;
    }

    // ==================== GETTERS ====================

    public String getPassengerId()       { return passengerId; }
    public String getName()              { return name; }
    public String getSurname()           { return surname; }
    public String getFullName()          { return name + " " + surname; }
    public PassengerClass getPassengerClass() { return passengerClass; }
    public String getFlightNumber()      { return flightNumber; }
    public String getNationalId()        { return nationalId; }

    // ==================== SETTERS ====================

    public void setName(String name)               { this.name = name; }
    public void setSurname(String surname)         { this.surname = surname; }
    public void setPassengerClass(PassengerClass c){ this.passengerClass = c; }
    public void setFlightNumber(String fn)         { this.flightNumber = fn; }
    public void setNationalId(String id)           { this.nationalId = id; }

    // ==================== OBJECT METHODS ====================

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Passenger)) return false;
        Passenger p = (Passenger) o;
        return Objects.equals(passengerId, p.passengerId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(passengerId);
    }

    @Override
    public String toString() {
        return String.format("Passenger{id='%s', name='%s %s', class=%s, flight='%s'}",
                passengerId, name, surname, passengerClass, flightNumber);
    }
}