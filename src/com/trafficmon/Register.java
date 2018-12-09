package com.trafficmon;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class Register {
    private final List<ZoneBoundaryCrossing> eventLog = new ArrayList<>();
    private BigDecimal charge;

    public void setCharge(BigDecimal charge) {
        this.charge = charge;
    }

    public BigDecimal getCharge() {
        return charge;
    }

    public List<ZoneBoundaryCrossing> getEventLog() {
        return eventLog;
    }

    public void addToList (ZoneBoundaryCrossing zoneBoundaryCrossing){
        eventLog.add(zoneBoundaryCrossing);
    }

    public void addEntryToList (Vehicle vehicle){
        eventLog.add(new EntryEvent(vehicle));
    }

    private boolean checkOrderingOf() {
        ZoneBoundaryCrossing lastEvent = eventLog.get(0);
        if (lastEvent instanceof ExitEvent) return false;
        for (ZoneBoundaryCrossing crossing : eventLog.subList(1, eventLog.size())) {
            if (crossing.timestamp().compareTo(lastEvent.timestamp()) < 0 )  return false;
            if (crossing.getClass().equals(lastEvent.getClass())) return false;
            lastEvent = crossing;
        }
        return !(lastEvent instanceof EntryEvent);
    }

    public boolean getOrdering(){
        return checkOrderingOf();
    }


    public void addExitToList(Vehicle vehicle) {
        eventLog.add(new ExitEvent(vehicle));
    }
}
