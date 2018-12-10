package com.trafficmon;

import org.jmock.Expectations;
import org.junit.Test;

public class ClockTests extends Testable{
    @Test
    public void testsClock() {
        context.checking(new Expectations() {{
            exactly(1).of(clock).now();
        }});
        eventLogEntry(new EntryEvent(vehicleOne, clock));

    }

    @Test
    public void testsClockNotUsed() {
        context.checking(new Expectations() {{
            exactly(0).of(clock).now();
        }});
        eventLogEntry(new EntryEvent(vehicleOne));

    }
}
