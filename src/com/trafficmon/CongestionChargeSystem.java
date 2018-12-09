package com.trafficmon;

import java.math.BigDecimal;
import java.util.*;

public class CongestionChargeSystem {

    private Map<Vehicle, Register> vehicleRegistration = new HashMap<>();

    public Map<Vehicle, Register> getVehicleRegistration() {
        return vehicleRegistration;
    }


    public void vehicleEnteringZone(Vehicle vehicle) {
        if(!vehicleRegistration.containsKey(vehicle)) {
            vehicleRegistration.put(vehicle, new Register());
        }
        vehicleRegistration.get(vehicle).addToList(new EntryEvent(vehicle));
    }

    public void vehicleLeavingZone(Vehicle vehicle) {
        if(vehicleRegistration.containsKey(vehicle)) {
            vehicleRegistration.get(vehicle).addToList(new ExitEvent(vehicle));
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


    public void calculateCharges() {

        for (Map.Entry<Vehicle, Register> vehicleCrossings : vehicleRegistration.entrySet()) {
            Vehicle vehicle = vehicleCrossings.getKey();
            List<ZoneBoundaryCrossing> crossings = vehicleCrossings.getValue().getEventLog();

            if (!vehicleCrossings.getValue().getOrdering(crossings)) {
                OperationsTeam.getInstance().triggerInvestigationInto(vehicle);
            } else {
                BigDecimal charge = new ChargeCalculator().getCharge(crossings);
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
