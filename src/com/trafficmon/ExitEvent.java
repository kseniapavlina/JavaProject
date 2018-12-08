package com.trafficmon;

public class ExitEvent extends ZoneBoundaryCrossing {
    public ExitEvent(Vehicle vehicleRegistration) {
        super(vehicleRegistration);
    }
    public ExitEvent(Vehicle vehicleRegistration, Clock clock) {
        super(vehicleRegistration, clock);
    }
}
