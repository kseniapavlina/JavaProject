package com.trafficmon;

import java.math.BigDecimal;
import java.util.*;

public class CongestionChargeSystem {

    private Map<Vehicle, BigDecimal> vehicleCharges = new HashMap<>();

    private final List<ZoneBoundaryCrossing> eventLog = new ArrayList<>();

    public void vehicleEnteringZone(Vehicle vehicle) {
        eventLog.add(new EntryEvent(vehicle));
    }

    public void vehicleLeavingZone(Vehicle vehicle) {
        if (!new Register().previouslyRegistered(vehicle, eventLog)) {
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

            if (!new Register().checkOrderingOf(crossings)) {
                OperationsTeam.getInstance().triggerInvestigationInto(vehicle);
            } else {
                BigDecimal charge = new ChargeCalculator().getCharge(crossings);

                vehicleCharges.put(vehicle, charge);
                try {
                    RegisteredCustomerAccountsService.getInstance().accountFor(vehicle).deduct(charge);
                } catch (InsufficientCreditException | AccountNotRegisteredException ice) {
                    OperationsTeam.getInstance().issuePenaltyNotice(vehicle, charge);
                }
            }
        }
    }
}
