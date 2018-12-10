package com.trafficmon;

import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Rule;
import org.junit.Test;

public class ClockTests extends TestHelper {

    @Rule
    public JUnitRuleMockery context = new JUnitRuleMockery();
    private final Clock clock = context.mock(Clock.class);

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
