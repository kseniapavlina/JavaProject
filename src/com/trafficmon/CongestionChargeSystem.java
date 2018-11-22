package com.trafficmon;

import java.math.BigDecimal;
import java.util.*;

public class CongestionChargeSystem {

    public static final BigDecimal CHARGE_RATE_POUNDS_PER_MINUTE = new BigDecimal(0.05);

//  a list of a times a vehicle enters
    private final List<ZoneBoundaryCrossing> eventLog = new ArrayList<ZoneBoundaryCrossing>();

//  add to the list
    public void vehicleEnteringZone(Vehicle vehicle) {
        eventLog.add(new EntryEvent(vehicle));
    }

//  checks to see if vehile has entered before or not and then adds it to exit event
    public void vehicleLeavingZone(Vehicle vehicle) {
        if (!previouslyRegistered(vehicle)) {
            return;
        }
        eventLog.add(new ExitEvent(vehicle));
    }

    public void calculateCharges() {
//doesn't calculate shit, calls another method in the end, FML
//      a map with within a list => give each vehicle a specific entry stamp
        Map<Vehicle, List<ZoneBoundaryCrossing>> crossingsByVehicle = new HashMap<Vehicle, List<ZoneBoundaryCrossing>>();

        for (ZoneBoundaryCrossing crossing : eventLog) {    // event log is the list we have at the top
//           creaates an empty list
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

    private BigDecimal calculateChargeForTimeInZone(List<ZoneBoundaryCrossing> crossings) {
        //time for some maths bitches (viki's voice)!

        //Java class - default value is 0 so you don't get charge.
        BigDecimal charge = new BigDecimal(0);

        //if we flip the list the last event would be the event that happened first.
        ZoneBoundaryCrossing lastEvent = crossings.get(0);
        //this for loop checks exit time and calculates the amount charged. You could leave and enter more than once.
        for (ZoneBoundaryCrossing crossing : crossings.subList(1, crossings.size())) {
            // calculates the charge
            if (crossing instanceof ExitEvent) {
                charge = charge.add(
                        new BigDecimal(minutesBetween(lastEvent.timestamp(), crossing.timestamp()))
                                .multiply(CHARGE_RATE_POUNDS_PER_MINUTE));
            }

            lastEvent = crossing;
        }

        return charge;
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
        //crossing = every time recorded(list)
        //last event = the first time recorded
        ZoneBoundaryCrossing lastEvent = crossings.get(0);

        for (ZoneBoundaryCrossing crossing : crossings.subList(1, crossings.size())) {
            // timestamp : when cars entered or exited
            if (crossing.timestamp() < lastEvent.timestamp()) {
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

        return true;
    }
    //Quick Maffs
    private int minutesBetween(long startTimeMs, long endTimeMs) {
        return (int) Math.ceil((endTimeMs - startTimeMs) / (1000.0 * 60.0));
    }
}
