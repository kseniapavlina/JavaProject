package com.trafficmon;

import junit.framework.TestCase;
import org.junit.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

import static junit.framework.TestCase.assertEquals;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertFalse;

public class RegisterTests extends TestHelper {

    @Test
    public void testsOrderingIsFalse(){
        eventLogEntry(new EntryEvent(vehicleOne, getControllableClock(9, 0, 0)));
        eventLogEntry(new EntryEvent(vehicleOne, getControllableClock(10, 0, 0)));
        assertFalse(getOrdering(vehicleOne));
    }

    @Test
    public void testsOrderingIsTrue(){
        eventLogEntry(new EntryEvent(vehicleOne, getControllableClock(9, 0, 0)));
        eventLogEntry(new ExitEvent(vehicleOne, getControllableClock(10, 0, 0)));
        TestCase.assertTrue(getOrdering(vehicleOne));

    }

    @Test
    public void testsOrderingIsFalseWhenStartWithExit(){
        eventLogEntry(new ExitEvent(vehicleOne, getControllableClock(12, 0, 0)));
        eventLogEntry(new EntryEvent(vehicleOne, getControllableClock(13, 0, 0)));
        TestCase.assertFalse(getOrdering(vehicleOne));

    }

    @Test
    public void testsTimer() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        eventLogEntry(new EntryEvent(vehicleOne, getControllableClock(9, 0, 0)));
        eventLogEntry(new ExitEvent(vehicleOne, getControllableClock(11, 0, 0)));
        eventLogEntry(new EntryEvent(vehicleOne, getControllableClock(12, 0, 0)));
        eventLogEntry(new ExitEvent(vehicleOne, getControllableClock(14, 30, 0)));
        ChargeCalculator chargeCalculator = new ChargeCalculator();
        Method method = chargeCalculator.getClass().getDeclaredMethod("timer", List.class);
        method.setAccessible(true);
        assertEquals(
                method.invoke(chargeCalculator, congestionChargeSystem.getCompleteEventLog()),
                4.5
        );
    }

    @Test
    public void testsOrderingLastIsEntry(){
        eventLogEntry(new EntryEvent(vehicleOne, getControllableClock(9, 0, 0)));
        eventLogEntry(new ExitEvent(vehicleOne, getControllableClock(11, 0, 0)));
        eventLogEntry(new EntryEvent(vehicleOne, getControllableClock(12, 0, 0)));
        eventLogEntry(new ExitEvent(vehicleOne, getControllableClock(14, 30, 0)));
        eventLogEntry(new EntryEvent(vehicleOne, getControllableClock(16, 0, 0)));
        assertFalse(getOrdering(vehicleOne));

    }

    @Test
    public void testsOrderingFirstIsExit(){
        eventLogEntry(new ExitEvent(vehicleOne, getControllableClock(11, 0, 0)));
        eventLogEntry(new EntryEvent(vehicleOne, getControllableClock(12, 0, 0)));
        eventLogEntry(new ExitEvent(vehicleOne, getControllableClock(14, 0, 0)));
        eventLogEntry(new EntryEvent(vehicleOne, getControllableClock(16, 0, 0)));
        assertFalse(getOrdering(vehicleOne));
    }
}
