package com.trafficmon;

import java.util.List;

public class Register {

    boolean previouslyRegistered(Vehicle vehicle, List<ZoneBoundaryCrossing> eventLog) {
        for (ZoneBoundaryCrossing crossing : eventLog) {
            if (crossing.getVehicle().equals(vehicle)) {
                return true;
            }
        }
        return false;
    }

    public boolean isRegistered(Vehicle vehicle, List <ZoneBoundaryCrossing> eventLog){
        return previouslyRegistered(vehicle, eventLog);
    }

    boolean checkOrderingOf(List<ZoneBoundaryCrossing> crossings) {
        ZoneBoundaryCrossing lastEvent = crossings.get(0);
        if (lastEvent instanceof ExitEvent) return false;
        for (ZoneBoundaryCrossing crossing : crossings.subList(1, crossings.size())) {
            if (crossing.timestamp().compareTo(lastEvent.timestamp()) < 0 )  return false;
            if (crossing.getClass().equals(lastEvent.getClass())) return false;
            lastEvent = crossing;
        }
        return !(lastEvent instanceof EntryEvent);
    }

    public boolean getOrdering(List<ZoneBoundaryCrossing> crossings){
        return checkOrderingOf(crossings);
    }
}
