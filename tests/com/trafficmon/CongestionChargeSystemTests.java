package com.trafficmon;

import org.junit.Test;

import java.math.BigDecimal;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class CongestionChargeSystemTests extends TestHelper {
    @Test
    public void testsIsRegistered(){
        eventLogEntry(new EntryEvent(vehicleOne, getControllableClock(13, 0, 0)));
        eventLogEntry(new ExitEvent(vehicleOne, getControllableClock(14, 0, 0)));
        assertTrue(congestionChargeSystem.isVehicleRegistered(vehicleOne));
        assertFalse(congestionChargeSystem.isVehicleRegistered(vehicleTwo));
    }

    @Test
    public void testsLeavingRegisteredInEventLog(){
        congestionChargeSystem.vehicleLeavingZone(vehicleOne);
        assertThat(congestionChargeSystem.getCompleteEventLog().size(), is(0));
        congestionChargeSystem.vehicleEnteringZone(vehicleOne);
        congestionChargeSystem.vehicleLeavingZone(vehicleOne);
        assertThat(congestionChargeSystem.getCompleteEventLog().size(), is(2));
    }

    @Test
    public void testsEventLogZeroValue(){
        congestionChargeSystem.vehicleLeavingZone(vehicleTwo);
        congestionChargeSystem.vehicleEnteringZone(vehicleOne);
        congestionChargeSystem.vehicleLeavingZone(vehicleOne);
        ZoneBoundaryCrossing z = congestionChargeSystem.getCompleteEventLog().get(0);
        assertThat(z.getVehicle(), is(vehicleOne));
    }
}
