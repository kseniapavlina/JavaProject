package com.trafficmon;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

public class CongestionChargeSystem {

    private Map<Vehicle, BigDecimal> THE_CHARGE = new HashMap<>();
    private static final LocalTime TIME_BOUNDARY = LocalTime.of(14,0,0);
    private static final BigDecimal LOWER_FEE = new BigDecimal(4);
    private static final BigDecimal UPPER_FEE = new BigDecimal(6);
    private static final BigDecimal LONG_FEE = new BigDecimal(12);
    private static final int HOUR_BETWEEN = 4;

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

                BigDecimal charge = calculateChargeForTimeInZone(crossings);
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

    public Map charge2(){
        return THE_CHARGE;
    }

    private BigDecimal calculateChargeForTimeInZone(List<ZoneBoundaryCrossing> crossings){
        ZoneBoundaryCrossing lastEvent = crossings.get(0);
        ArrayList<LocalTime> timesToCharge = new ArrayList<>();
        LocalTime criticalTime = crossings.get(1).timestamp();
        timesToCharge.add(lastEvent.timestamp());
        BigDecimal charge = new BigDecimal(0);
        if (timer(crossings) < 4){
            for (ZoneBoundaryCrossing crossing : crossings.subList(2, crossings.size())){
                if (crossing instanceof EntryEvent){
                    if (hoursBetween(criticalTime, crossing.timestamp()) > 4){
                        int i = crossings.indexOf(crossing);
                        criticalTime = crossings.get(i).timestamp();
                        timesToCharge.add(crossing.timestamp());
                    }
                }
            }
            for (LocalTime time : timesToCharge){
                if (time.compareTo(TIME_BOUNDARY) <= 0) charge = charge.add(UPPER_FEE);
                else charge = charge.add(LOWER_FEE);
            }
        }
        else charge = charge.add(LONG_FEE);
        return charge;
    }


    public double timer(List<ZoneBoundaryCrossing> crossings){
        double timer = 0;
        for (int i = 0; i < crossings.size()-1; i+=2){
            timer += hoursBetween(crossings.get(i).timestamp(), crossings.get(i+1).timestamp());
        }
        return timer;
    }

    private boolean previouslyRegistered(Vehicle vehicle) {
        for (ZoneBoundaryCrossing crossing : eventLog) {
            if (crossing.getVehicle().equals(vehicle)) {
                return true;
            }
        }
        return false;
    }

    private boolean checkOrderingOf(List<ZoneBoundaryCrossing> crossings) {
        ZoneBoundaryCrossing lastEvent = crossings.get(0);
        if (lastEvent instanceof  ExitEvent) return false;
        for (ZoneBoundaryCrossing crossing : crossings.subList(1, crossings.size())) {
            if (crossing.timestamp().compareTo(lastEvent.timestamp()) < 0 ) {
                return false;
            }
            if (crossing instanceof EntryEvent && lastEvent instanceof EntryEvent) {
                return false;
            }
            if (crossing instanceof ExitEvent && lastEvent instanceof ExitEvent) {
                return false;
            }
            lastEvent = crossing;
        }
        return !(lastEvent instanceof EntryEvent);
    }

    public boolean getOrdering(List<ZoneBoundaryCrossing> crossings){
        return checkOrderingOf(crossings);
    }


    //Quick Maffs
    private long hoursBetween(LocalTime startTimeMs, LocalTime endTimeMs) {
        return (int) Math.ceil(ChronoUnit.HOURS.between(startTimeMs, endTimeMs));
    }

    public long getter(LocalTime startTimeMs, LocalTime endTimeMs){
        return hoursBetween(startTimeMs, endTimeMs);
    }
}
