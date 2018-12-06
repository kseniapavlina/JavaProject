package com.trafficmon;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.*;

public class CongestionChargeSystem {

    private Map<Vehicle, BigDecimal> THE_CHARGE = new HashMap<>();

    private final List<ZoneBoundaryCrossing> eventLog = new ArrayList<>();

    public void vehicleEnteringZone(Vehicle vehicle) {
        eventLog.add(new EntryEvent(vehicle));
    }

    public void vehicleLeavingZone(Vehicle vehicle) {
        if (!previouslyRegistered(vehicle)) {
            return;
        }
        eventLog.add(new ExitEvent(vehicle));
    }

    public List<ZoneBoundaryCrossing> getEventLog(){
        return eventLog;
    }

    public void calculateCharges() {
        Map<Vehicle, List<ZoneBoundaryCrossing>> crossingsByVehicle = new HashMap<>();

        for (ZoneBoundaryCrossing crossing : eventLog) {
            if (!crossingsByVehicle.containsKey(crossing.getVehicle())) {
                crossingsByVehicle.put(crossing.getVehicle(), new ArrayList<>());
            }
            crossingsByVehicle.get(crossing.getVehicle()).add(crossing);
        }
        for (Map.Entry<Vehicle, List<ZoneBoundaryCrossing>> vehicleCrossings : crossingsByVehicle.entrySet()) {
            Vehicle vehicle = vehicleCrossings.getKey();
            List<ZoneBoundaryCrossing> crossings = vehicleCrossings.getValue();

            if (!checkOrderingOf(crossings)) {
                OperationsTeam.getInstance().triggerInvestigationInto(vehicle);
            }
            else {

                BigDecimal charge = new ChargeCalculator().getCharge(crossings);

                THE_CHARGE.put(vehicle, charge);
                try {
                    RegisteredCustomerAccountsService.getInstance().accountFor(vehicle).deduct(charge);
                }
                catch (InsufficientCreditException | AccountNotRegisteredException ice) {
                    OperationsTeam.getInstance().issuePenaltyNotice(vehicle, charge);
                }

            }
        }
    }

    public Map getChargeMap(){
        return THE_CHARGE;
    }


    private boolean previouslyRegistered(Vehicle vehicle) {
        for (ZoneBoundaryCrossing crossing : eventLog) {
            if (crossing.getVehicle().equals(vehicle)) {
                return true;
            }
        }
        return false;
    }

    public boolean isRegistered(Vehicle vehicle){
        return previouslyRegistered(vehicle);
    }

    private boolean checkOrderingOf(List<ZoneBoundaryCrossing> crossings) {
        ZoneBoundaryCrossing lastEvent = crossings.get(0);
        if (lastEvent instanceof  ExitEvent) return false;
        for (ZoneBoundaryCrossing crossing : crossings.subList(1, crossings.size())) {
            if (compareTime(crossing.timestamp(), lastEvent.timestamp()) < 0 )  return false;
            if (crossing.getClass().equals(lastEvent.getClass())) return false;
            lastEvent = crossing;
        }
        return !(lastEvent instanceof EntryEvent);
    }

    public boolean getOrdering(List<ZoneBoundaryCrossing> crossings){
        return checkOrderingOf(crossings);
    }

    // FIXME: 06/12/2018 we have two compareTime methods (here and ChargeCalculator)
    private int compareTime(LocalTime x, LocalTime y){
        return x.compareTo(y);
    }
}
