package com.trafficmon;

public class ExitEvent extends ZoneBoundaryCrossing {
    public ExitEvent(Vehicle vehicle) {
        super(vehicle);
    }
    public ExitEvent(Vehicle vehicleRegistration, Clock clock) {
        super(vehicleRegistration, clock);
    }
}
