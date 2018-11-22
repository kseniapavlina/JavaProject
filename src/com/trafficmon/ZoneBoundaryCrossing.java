package com.trafficmon;

// why is this abstract? cant you just make an instance of this?
public abstract class ZoneBoundaryCrossing {

    private final Vehicle vehicle;
    private final long time;

    public ZoneBoundaryCrossing(Vehicle vehicle) {
        this.vehicle = vehicle;
        this.time = System.currentTimeMillis();
    }

    public Vehicle getVehicle() {
        return vehicle;
    }

    public long timestamp() {
        return time;
    }
}
