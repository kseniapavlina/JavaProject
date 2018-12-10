package com.trafficmon;

import java.math.BigDecimal;
import java.util.*;

public class CongestionChargeSystem {

    private final Map<Vehicle, Register> vehicleRegistration = new HashMap<>();
    private final Chargeable chargeable;
    private final LibraryChecker libraryChecker;

    CongestionChargeSystem() {
        this.libraryChecker = new Trigger();
        this.chargeable = new ChargeCalculator();
    }

    CongestionChargeSystem(LibraryChecker libraryChecker){
        this.libraryChecker = libraryChecker;
        this.chargeable = new ChargeCalculator();
    }


    CongestionChargeSystem(Chargeable chargeable){
        this.libraryChecker = new Trigger();
        this.chargeable = chargeable;
    }


    Map<Vehicle, Register> getVehicleRegistration() {
        return vehicleRegistration;
    }

    void vehicleEnteringZone(Vehicle vehicle) {
        if(!isVehicleRegistered(vehicle)) {
            vehicleRegistration.put(vehicle, new Register());
        }
        vehicleRegistration.get(vehicle).addEntryToList(vehicle);
    }

    void vehicleLeavingZone(Vehicle vehicle) {
        if(isVehicleRegistered(vehicle)) {
            vehicleRegistration.get(vehicle).addExitToList(vehicle);
        }
    }

    List<ZoneBoundaryCrossing> getCompleteEventLog(){
        List<ZoneBoundaryCrossing> completeEventLog = new ArrayList<>();

        for(Map.Entry<Vehicle, Register> vehicleCrossings : vehicleRegistration.entrySet()){
            List<ZoneBoundaryCrossing> crossings = vehicleCrossings.getValue().getEventLog();
            completeEventLog.addAll(crossings);
        }

        return completeEventLog;
    }

    boolean isVehicleRegistered(Vehicle vehicle){
        return vehicleRegistration.containsKey(vehicle);
    }

    void calculateCharges() {
        for (Map.Entry<Vehicle, Register> vehicleCrossings : vehicleRegistration.entrySet()) {
            Vehicle vehicle = vehicleCrossings.getKey();
            List<ZoneBoundaryCrossing> crossings = vehicleCrossings.getValue().getEventLog();

            if (!vehicleCrossings.getValue().getOrdering()) {
                libraryChecker.libraryTrigger().triggerInvestigationInto(vehicle);
            } else {
                BigDecimal charge = chargeable.calculateChargeForTimeInZone(crossings);
                vehicleRegistration.get(vehicle).setCharge(charge);
                libraryChecker.accountFor(vehicle, charge);
            }
        }
    }

}
