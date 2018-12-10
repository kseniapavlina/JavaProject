package com.trafficmon;

import org.junit.Test;

import java.time.LocalTime;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.number.OrderingComparison.greaterThan;

public class ZoneBoundaryCrossingTests extends TestHelper {
    @Test
    public void testsExitTimeStamp(){
        eventLogEntry(new EntryEvent(vehicleOne, getControllableClock(9, 0, 0)));
        eventLogEntry(new ExitEvent(vehicleOne, getControllableClock(10, 0, 0)));
        ZoneBoundaryCrossing crossingByOne = congestionChargeSystem.getCompleteEventLog().get(0);
        ZoneBoundaryCrossing crossingByTwo = congestionChargeSystem.getCompleteEventLog().get(1);
        assertThat(crossingByTwo.timestamp(), greaterThan(crossingByOne.timestamp()));
    }

    @Test
    public void testsEnteringTime(){
        eventLogEntry(new EntryEvent(vehicleOne, getControllableClock(9, 0, 0)));
        eventLogEntry(new ExitEvent(vehicleOne, getControllableClock(10, 0, 0)));
        LocalTime timestamp = congestionChargeSystem.getCompleteEventLog().get(0).timestamp();
        eventLogEntry(new EntryEvent(vehicleOne, getControllableClock(12, 0, 0)));
        ZoneBoundaryCrossing crossing = congestionChargeSystem.getCompleteEventLog().get(2);
        assertThat(crossing.timestamp(), greaterThan(timestamp));

    }
}
