package com.trafficmon;

import java.math.BigDecimal;
import java.util.*;

public class CongestionChargeSystem {

    private final Map<Vehicle, Register> vehicleRegistration = new HashMap<>();
    private final Chargeable chargeable = new ChargeCalculator();

    public Map<Vehicle, Register> getVehicleRegistration() {
        return vehicleRegistration;
    }

    public void vehicleEnteringZone(Vehicle vehicle) {
        if(!isVehicleRegistered(vehicle)) {
            vehicleRegistration.put(vehicle, new Register());
        }
        vehicleRegistration.get(vehicle).addEntryToList(vehicle);
    }

    public void vehicleLeavingZone(Vehicle vehicle) {
        if(isVehicleRegistered(vehicle)) {
            vehicleRegistration.get(vehicle).addExitToList(vehicle);
        }
    }

    public List<ZoneBoundaryCrossing> getCompleteEventLog(){
        List<ZoneBoundaryCrossing> completeEventLog = new ArrayList<>();

        for(Map.Entry<Vehicle, Register> vehicleCrossings : vehicleRegistration.entrySet()){
            List<ZoneBoundaryCrossing> crossings = vehicleCrossings.getValue().getEventLog();
            completeEventLog.addAll(crossings);
        }

        return completeEventLog;
    }

    public boolean isVehicleRegistered(Vehicle vehicle){
        return vehicleRegistration.containsKey(vehicle);
    }

    public void calculateCharges() {
        for (Map.Entry<Vehicle, Register> vehicleCrossings : vehicleRegistration.entrySet()) {
            Vehicle vehicle = vehicleCrossings.getKey();
            List<ZoneBoundaryCrossing> crossings = vehicleCrossings.getValue().getEventLog();

            if (!vehicleCrossings.getValue().getOrdering()) {
                OperationsTeam.getInstance().triggerInvestigationInto(vehicle);
            } else {
                BigDecimal charge = chargeable.calculateChargeForTimeInZone(crossings);
                vehicleRegistration.get(vehicle).setCharge(charge);
                try {
                    RegisteredCustomerAccountsService.getInstance().accountFor(vehicle).deduct(charge);
                } catch (InsufficientCreditException | AccountNotRegisteredException ice) {
                    OperationsTeam.getInstance().issuePenaltyNotice(vehicle, charge);
                }
            }
        }
    }

}
