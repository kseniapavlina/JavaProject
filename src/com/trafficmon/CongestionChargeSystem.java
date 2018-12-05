package com.trafficmon;

import javafx.util.converter.LocalDateStringConverter;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

public class CongestionChargeSystem {

    public static final double CHARGE_RATE_POUNDS_PER_MINUTE = 0.05;
    public Map<Vehicle, BigDecimal> THE_CHARGE = new HashMap<>();
    public static final LocalTime TIME_BOUNDARY = LocalTime.of(14,00,00);
    private static final BigDecimal LOWER_FEE = new BigDecimal(4);
    private static final BigDecimal UPPER_FEE = new BigDecimal(6);
    private static final BigDecimal LONG_FEE = new BigDecimal(12);

//  a list of a times a vehicle enters
    private final List<ZoneBoundaryCrossing> eventLog = new ArrayList<ZoneBoundaryCrossing>();

//  add to the list
    public void vehicleEnteringZone(Vehicle vehicle) {
        eventLog.add(new EntryEvent(vehicle));
    }

//  checks to see if vehicle has entered before or not and then adds it to exit event
    public void vehicleLeavingZone(Vehicle vehicle) {
        if (!previouslyRegistered(vehicle)) {
            return;
        }
        eventLog.add(new ExitEvent(vehicle));
    }

    //TO CHECK EVENTLOG NEED GETTER
    public List<ZoneBoundaryCrossing> getEventLog(){
        return eventLog;
    }

    public void calculateCharges() {
//doesn't calculate shit, calls another method in the end, FML
//      a map with within a list => give each vehicle a specific entry stamp
        Map<Vehicle, List<ZoneBoundaryCrossing>> crossingsByVehicle = new HashMap<Vehicle, List<ZoneBoundaryCrossing>>();

        for (ZoneBoundaryCrossing crossing : eventLog) {    // event log is the list we have at the top
//           creates an empty list
            if (!crossingsByVehicle.containsKey(crossing.getVehicle())) { //check if the vehicle is in the hashmap
                crossingsByVehicle.put(crossing.getVehicle(), new ArrayList<ZoneBoundaryCrossing>()); // if its not put in the map
            }
            // FIXME: 22/11/2018 this doesnt make sense
            crossingsByVehicle.get(crossing.getVehicle()).add(crossing);  //
        }
// for each car and list of crossings in the list
        for (Map.Entry<Vehicle, List<ZoneBoundaryCrossing>> vehicleCrossings : crossingsByVehicle.entrySet()) {
            //get the name of the car
            Vehicle vehicle = vehicleCrossings.getKey();
            //ALERT CROSSINGS APPEAR HERE VERY IMPORTANT last entry becomes 0 for some reason????
            List<ZoneBoundaryCrossing> crossings = vehicleCrossings.getValue();

            //check if crossings are in order trigger exception (in lib)
            if (!checkOrderingOf(crossings)) {
                OperationsTeam.getInstance().triggerInvestigationInto(vehicle);
            }
            //else we calculate the charge
            else {

                BigDecimal charge = calculateChargeForTimeInZone(crossings);
                THE_CHARGE.put(vehicle, charge);
                try {
//this is bs need to refactor
                    RegisteredCustomerAccountsService.getInstance().accountFor(vehicle).deduct(charge);
                }
                //catch exception for insufficient funds
                catch (InsufficientCreditException ice) {
                    OperationsTeam.getInstance().issuePenaltyNotice(vehicle, charge);
                }
                //catch unregistered account?? will it even get here
                catch (AccountNotRegisteredException e) {
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
                    if (minsBetween(criticalTime, crossing.timestamp()) / 60.0 > 4){
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


    //we assume this is called with events in right order
    public double timer(List<ZoneBoundaryCrossing> crossings){
        double timer = 0;
        for (int i = 0; i < crossings.size()-1; i+=2){
            timer += minsBetween(crossings.get(i).timestamp(), crossings.get(i+1).timestamp()) / 60.0;
        }
        return timer;
    }


    public BigDecimal getCharge(List<ZoneBoundaryCrossing> crossings){
        return calculateChargeForTimeInZone(crossings);
    }

    //checks if the vehicle has been registered before or not.
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
        if (lastEvent instanceof ExitEvent) return false;
        for (ZoneBoundaryCrossing crossing : crossings.subList(1, crossings.size())) {
            // timestamp : when cars entered or exited
            if (crossing.timestamp().compareTo(lastEvent.timestamp()) < 0 ) {
                return false;
            }
            if (crossing instanceof EntryEvent && lastEvent instanceof EntryEvent) {
                return false;
            }
            if (crossing instanceof ExitEvent && lastEvent instanceof ExitEvent) {
                return false;
            }
//            if (lastEvent instanceof EntryEvent){
//                return false;
//            }
//            if (crossing instanceof ExitEvent){
//                return false;
//            }
            lastEvent = crossing;
        }
        if (lastEvent instanceof  EntryEvent) return false;
        return true;
    }

    public boolean getOrdering(List<ZoneBoundaryCrossing> crossings){
        return checkOrderingOf(crossings);
    }


    //Quick Maffs
    private long minsBetween(LocalTime startTimeMs, LocalTime endTimeMs) {
        return (int) Math.ceil(ChronoUnit.MINUTES.between(startTimeMs, endTimeMs));
    }

    public long getter(LocalTime startTimeMs, LocalTime endTimeMs){
        return minsBetween(startTimeMs, endTimeMs);
    }
}
