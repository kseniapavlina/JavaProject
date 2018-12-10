package com.trafficmon;

import org.junit.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.LocalTime;

import static junit.framework.TestCase.assertEquals;

public class ChargeCalculatorTests extends TestHelper {

    @Test
    public void calculatesChargeForLongerThanFourHours(){
        eventLogEntry(new EntryEvent(vehicleOne, getControllableClock(9, 0, 0)));
        eventLogEntry(new EntryEvent(vehicleThree, getControllableClock(14, 30, 0)));
        eventLogEntry(new ExitEvent(vehicleOne, getControllableClock(18, 0, 0)));
        eventLogEntry(new ExitEvent(vehicleThree, getControllableClock(22, 0, 0)));
        congestionChargeSystem.calculateCharges();
        assertEquals(getVehicleCharge(vehicleOne), getVehicleCharge(vehicleThree));
    }

    @Test
    public void calculatesChargeForEntryAfterTwoLessThanFourHours(){
        eventLogEntry(new EntryEvent(vehicleOne, getControllableClock(15, 0, 0)));
        eventLogEntry(new ExitEvent(vehicleOne, getControllableClock(16, 0, 0)));
        congestionChargeSystem.calculateCharges();
        BigDecimal answer = new BigDecimal("4");
        assertEquals(getVehicleCharge(vehicleOne), answer);
    }

    @Test
    public void calculatesChargesWithLeaving(){
        eventLogEntry(new EntryEvent(vehicleOne, getControllableClock(9, 0, 0)));
        eventLogEntry(new ExitEvent(vehicleOne, getControllableClock(11, 0, 0)));
        eventLogEntry(new EntryEvent(vehicleOne, getControllableClock(12, 0, 0)));
        eventLogEntry(new ExitEvent(vehicleOne, getControllableClock(13, 0, 0)));
        eventLogEntry(new EntryEvent(vehicleOne, getControllableClock(19, 0, 0)));
        eventLogEntry(new ExitEvent(vehicleOne, getControllableClock(19, 30, 0)));
        congestionChargeSystem.calculateCharges();
        BigDecimal answer = new BigDecimal("10");
        assertEquals(getVehicleCharge(vehicleOne), answer);
    }

    @Test
    public void calculatesMultipleEntryLessThanFourHours(){
        eventLogEntry(new EntryEvent(vehicleOne, getControllableClock(9, 0, 0)));
        eventLogEntry(new ExitEvent(vehicleOne, getControllableClock(11, 0, 0)));
        eventLogEntry(new EntryEvent(vehicleOne, getControllableClock(16, 0, 0)));
        eventLogEntry(new ExitEvent(vehicleOne, getControllableClock(17, 0, 0)));
        eventLogEntry(new EntryEvent(vehicleOne, getControllableClock(22, 0, 0)));
        eventLogEntry(new ExitEvent(vehicleOne, getControllableClock(22, 30, 0)));
        congestionChargeSystem.calculateCharges();
        BigDecimal answer = new BigDecimal("14");
        assertEquals(getVehicleCharge(vehicleOne), answer);
    }

    @Test
    public void calculatesMultipleEntryLessThatFourHoursBeforeTwo(){
        eventLogEntry(new EntryEvent(vehicleOne, getControllableClock(9, 0, 0)));
        eventLogEntry(new ExitEvent(vehicleOne, getControllableClock(11, 0, 0)));
        eventLogEntry(new EntryEvent(vehicleOne, getControllableClock(12, 0, 0)));
        eventLogEntry(new ExitEvent(vehicleOne, getControllableClock(13, 0, 0)));
        congestionChargeSystem.calculateCharges();
        BigDecimal answer = new BigDecimal("6");
        assertEquals(getVehicleCharge(vehicleOne), answer);
    }

    @Test
    public void calculatesMultipleEntryLessThatFourHoursAfterTwo(){
        eventLogEntry(new EntryEvent(vehicleOne, getControllableClock(15, 0, 0)));
        eventLogEntry(new ExitEvent(vehicleOne, getControllableClock(16, 0, 0)));
        eventLogEntry(new EntryEvent(vehicleOne, getControllableClock(18, 0, 0)));
        eventLogEntry(new ExitEvent(vehicleOne, getControllableClock(19, 30, 0)));
        congestionChargeSystem.calculateCharges();
        BigDecimal answer = new BigDecimal("4");
        assertEquals(getVehicleCharge(vehicleOne), answer);
    }

    @Test
    public void calculatesMultipleEntryMoreThatFourHours(){
        eventLogEntry(new EntryEvent(vehicleOne, getControllableClock(9, 0, 0)));
        eventLogEntry(new ExitEvent(vehicleOne, getControllableClock(11, 0, 0)));
        eventLogEntry(new EntryEvent(vehicleOne, getControllableClock(15, 0, 0)));
        eventLogEntry(new ExitEvent(vehicleOne, getControllableClock(16, 0, 0)));
        eventLogEntry(new EntryEvent(vehicleOne, getControllableClock(18, 0, 0)));
        eventLogEntry(new ExitEvent(vehicleOne, getControllableClock(19, 30, 0)));
        congestionChargeSystem.calculateCharges();
        BigDecimal answer = new BigDecimal("12");
        assertEquals(getVehicleCharge(vehicleOne), answer);
    }

    @Test
    public void calculatesMultipleEntryAfterFourWithMoreThanFourBetween(){
        eventLogEntry(new EntryEvent(vehicleOne, getControllableClock(15, 0, 0)));
        eventLogEntry(new ExitEvent(vehicleOne, getControllableClock(16, 0, 0)));
        eventLogEntry(new EntryEvent(vehicleOne, getControllableClock(23, 0, 0)));
        eventLogEntry(new ExitEvent(vehicleOne, getControllableClock(23, 30, 0)));
        congestionChargeSystem.calculateCharges();
        BigDecimal answer = new BigDecimal("8");
        assertEquals(getVehicleCharge(vehicleOne), answer);
    }

    @Test
    public void calculatesMultipleEntryBeforeAndAfterTwo(){
        eventLogEntry(new EntryEvent(vehicleOne, getControllableClock(13, 0, 0)));
        eventLogEntry(new ExitEvent(vehicleOne, getControllableClock(14, 0, 0)));
        eventLogEntry(new EntryEvent(vehicleOne, getControllableClock(15, 0, 0)));
        eventLogEntry(new ExitEvent(vehicleOne, getControllableClock(15, 30, 0)));
        congestionChargeSystem.calculateCharges();
        BigDecimal answer = new BigDecimal("6");
        assertEquals(getVehicleCharge(vehicleOne), answer);
    }

    @Test
    public void calculatesEntryTwoCars(){
        eventLogEntry(new EntryEvent(vehicleOne, getControllableClock(12, 0, 0)));
        eventLogEntry(new EntryEvent(vehicleTwo, getControllableClock(13, 0, 0)));
        eventLogEntry(new ExitEvent(vehicleOne, getControllableClock(14, 0, 0)));
        eventLogEntry(new ExitEvent(vehicleTwo, getControllableClock(18, 30, 0)));

        congestionChargeSystem.calculateCharges();
        BigDecimal answer1 = new BigDecimal("6");
        BigDecimal answer2 = new BigDecimal("12");
        assertEquals(getVehicleCharge(vehicleOne), answer1);
        assertEquals(getVehicleCharge(vehicleTwo), answer2);
    }

    @Test
    public void calculatesTwoCarsMultipleEntry(){
        eventLogEntry(new EntryEvent(vehicleOne, getControllableClock(13, 0, 0)));
        eventLogEntry(new ExitEvent(vehicleOne, getControllableClock(14, 0, 0)));
        eventLogEntry(new EntryEvent(vehicleOne, getControllableClock(15, 0, 0)));
        eventLogEntry(new ExitEvent(vehicleOne, getControllableClock(15, 30, 0)));

        eventLogEntry(new EntryEvent(vehicleTwo, getControllableClock(15, 0, 0)));
        eventLogEntry(new ExitEvent(vehicleTwo, getControllableClock(16, 0, 0)));
        eventLogEntry(new EntryEvent(vehicleTwo, getControllableClock(23, 0, 0)));
        eventLogEntry(new ExitEvent(vehicleTwo, getControllableClock(23, 30, 0)));

        congestionChargeSystem.calculateCharges();

        BigDecimal answer1 = new BigDecimal("6");
        BigDecimal answer2 = new BigDecimal("8");
        assertEquals(getVehicleCharge(vehicleOne), answer1);
        assertEquals(getVehicleCharge(vehicleTwo), answer2);
    }

    @Test
    public void calculatesChargeForEntryBeforeTwoLessThanFourHours() {
        eventLogEntry(new EntryEvent(vehicleOne, getControllableClock(9, 0, 0)));
        eventLogEntry(new ExitEvent(vehicleOne, getControllableClock(11, 0, 0)));
        congestionChargeSystem.calculateCharges();
        BigDecimal answer = new BigDecimal("6");
        assertEquals(getVehicleCharge(vehicleOne), answer);
    }

    @Test
    public void testsHoursBetweenCalculation() throws InvocationTargetException, IllegalAccessException, NoSuchMethodException {
        ChargeCalculator chargeCalculator = new ChargeCalculator();
        Method method = chargeCalculator.getClass().getDeclaredMethod("calculateHoursBetween", LocalTime.class, LocalTime.class);
        method.setAccessible(true);
        LocalTime startTimeMs = LocalTime.of(9,10,50);
        LocalTime endTimeMs = LocalTime.of(10,11,50);
        assertEquals(
                61.0/60.0,
                method.invoke(chargeCalculator, startTimeMs, endTimeMs)
        );
    }
}
