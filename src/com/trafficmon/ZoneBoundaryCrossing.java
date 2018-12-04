package com.trafficmon;

import java.time.LocalTime;

// why is this abstract? cant you just make an instance of this?
public abstract class ZoneBoundaryCrossing {

    private final Vehicle vehicle;
    private final LocalTime time;

    public ZoneBoundaryCrossing(Vehicle vehicle) {
        this.vehicle = vehicle;
        this.time = LocalTime.now();
    }

    public ZoneBoundaryCrossing(Vehicle vehicle, LocalTime time) {
        this.vehicle = vehicle;
        this.time = time;
    }

    public Vehicle getVehicle() {
        return vehicle;
    }

    public LocalTime timestamp() {
        return time;
    }
}
