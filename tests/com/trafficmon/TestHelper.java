package com.trafficmon;

import java.math.BigDecimal;
import java.math.MathContext;
import java.time.LocalTime;

 class TestHelper {

     final Vehicle vehicleOne = Vehicle.withRegistration("A123 XYZ");
     final Vehicle vehicleTwo = Vehicle.withRegistration("B123 XYZ");
     final Vehicle vehicleThree = Vehicle.withRegistration("J091 4PY");
     final Vehicle vehicleOneCopy = Vehicle.withRegistration("A123 XYZ");

     private final TestHelper.ControllableClock controllableClock = new TestHelper.ControllableClock();

     final CongestionChargeSystem congestionChargeSystem = new CongestionChargeSystem();

     BigDecimal getVehicleCharge(Vehicle vehicle) {
        BigDecimal vehicleCharge = congestionChargeSystem.getVehicleRegistration().get(vehicle).getCharge();
        return vehicleCharge.round(new MathContext(2));
    }

     void eventLogEntry(ZoneBoundaryCrossing crossing){
        Vehicle v = crossing.getVehicle();
        if(!congestionChargeSystem.getVehicleRegistration().containsKey(v)){
            congestionChargeSystem.getVehicleRegistration().put(v, new Register());
        }
        congestionChargeSystem.getVehicleRegistration().get(v).addToList(crossing);
    }

     TestHelper.ControllableClock getControllableClock(int hr, int min, int sec){
        controllableClock.currentTimeIs(hr, min, sec);
        return controllableClock;
    }

     protected class ControllableClock implements Clock {
        private LocalTime now;

        @Override
        public LocalTime now() {
            return now;
        }

        private void currentTimeIs(int hour, int min, int sec) {
            now = LocalTime.of(hour,min,sec);
        }
    }

     boolean getOrdering(Vehicle vehicle){
        return congestionChargeSystem.getVehicleRegistration().get(vehicle).getOrdering();
    }
}
