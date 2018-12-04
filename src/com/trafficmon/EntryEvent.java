package com.trafficmon;

import java.time.LocalTime;

// do we even need this class?
public class EntryEvent extends ZoneBoundaryCrossing {
    public EntryEvent(Vehicle vehicleRegistration) {
        super(vehicleRegistration);
    }
    public EntryEvent(Vehicle vehicleRegistration, LocalTime time) {
        super(vehicleRegistration, time);
    }
}
