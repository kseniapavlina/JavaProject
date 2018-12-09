package com.trafficmon;

import static junit.framework.TestCase.assertEquals;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.number.OrderingComparison.greaterThan;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Rule;
import org.junit.rules.ExpectedException;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.MathContext;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Tests {

    private Vehicle vehicleOne = Vehicle.withRegistration("A123 XYZ");
    private Vehicle vehicleTwo = Vehicle.withRegistration("B123 XYZ");
    private Vehicle vehicleThree = Vehicle.withRegistration("J091 4PY");
    private Vehicle vehicleOneCopy = Vehicle.withRegistration("A123 XYZ");
    private ControllableClock controllableClock = new ControllableClock();

    private CongestionChargeSystem congestionChargeSystem = new CongestionChargeSystem();

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Rule
    public JUnitRuleMockery context = new JUnitRuleMockery();

    private Clock clock = context.mock(Clock.class);


    private void eventLogEntry(ZoneBoundaryCrossing crossing){
        Vehicle v = crossing.getVehicle();
        if(!congestionChargeSystem.getVehicleRegistration().containsKey(v)){
            congestionChargeSystem.getVehicleRegistration().put(v, new Register());
        }
        congestionChargeSystem.getVehicleRegistration().get(v).addToList(crossing);
    }

    private ControllableClock getControllableClock(int hr, int min, int sec){
        controllableClock.currentTimeIs(hr, min, sec);
        return controllableClock;
    }

    private class ControllableClock implements Clock {
        private LocalTime now;

        @Override
        public LocalTime now() {
            return now;
        }

        private void currentTimeIs(int hour, int min, int sec) {
            now = LocalTime.of(hour,min,sec);
        }
    }

    private BigDecimal getVehicleCharge(Vehicle vehicle) {
        Field field = null;
        try {
            field = CongestionChargeSystem.class.getDeclaredField("vehicleRegistration");
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }
        assert field != null;
        field.setAccessible(true);
        HashMap<Vehicle, Register> vehicleRegistration = null;
        try {
            vehicleRegistration = (HashMap<Vehicle, Register>)field.get(congestionChargeSystem);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        assert vehicleRegistration != null;
        BigDecimal vehicleCharge = vehicleRegistration.get(vehicle).getCharge();
        return vehicleCharge.round(new MathContext(2));
    }

    @Test
    public void checksGetRegistration(){
        assertEquals(vehicleOne.getRegistration(), "A123 XYZ");
        assertNotEquals(vehicleTwo.getRegistration(), "A123 XYZ");
    }

    @Test
    public void checkVehicleToSting(){
        assertEquals(vehicleOne.toString(), "Vehicle [A123 XYZ]");
    }

    @Test
    public void checkVehicleEquals(){
        assertThat(vehicleOne.equals(vehicleOneCopy), is(true));
        assertThat(vehicleOne.equals(vehicleThree), is(false));
    }

    @Test
    public void assertVehicleHashCode(){
        assertEquals(vehicleOne.hashCode(), vehicleOneCopy.hashCode());
        assertNotEquals(vehicleOne.hashCode(), vehicleThree.hashCode());
        assertNotEquals(vehicleOne.hashCode(), 0);
        assertEquals(Vehicle.withRegistration(null).hashCode(), 0);
    }

    @Test
    public void assertExitTimeStamp(){
        eventLogEntry(new EntryEvent(vehicleOne, getControllableClock(9, 0, 0)));
        eventLogEntry(new ExitEvent(vehicleOne, getControllableClock(10, 0, 0)));
        ZoneBoundaryCrossing crossingByOne = congestionChargeSystem.getCompleteEventLog().get(0);
        ZoneBoundaryCrossing crossingByTwo = congestionChargeSystem.getCompleteEventLog().get(1);
        assertThat(crossingByTwo.timestamp(), greaterThan(crossingByOne.timestamp()));
    }

    @Test
    public void assertEnteringTime(){
        eventLogEntry(new EntryEvent(vehicleOne, getControllableClock(9, 0, 0)));
        eventLogEntry(new ExitEvent(vehicleOne, getControllableClock(10, 0, 0)));
        LocalTime timestamp = congestionChargeSystem.getCompleteEventLog().get(0).timestamp();
        eventLogEntry(new EntryEvent(vehicleOne, getControllableClock(12, 0, 0)));
        ZoneBoundaryCrossing crossing = congestionChargeSystem.getCompleteEventLog().get(2);
        assertThat(crossing.timestamp(), greaterThan(timestamp));

    }

    @Test
    public void isLeavingRegisteredInEventLog(){
        congestionChargeSystem.vehicleLeavingZone(vehicleOne);
        assertThat(congestionChargeSystem.getCompleteEventLog().size(), is(0));
        congestionChargeSystem.vehicleEnteringZone(vehicleOne);
        congestionChargeSystem.vehicleLeavingZone(vehicleOne);
        assertThat(congestionChargeSystem.getCompleteEventLog().size(), is(2));
    }

    @Test
    public void testEventLogZeroValue(){
        congestionChargeSystem.vehicleLeavingZone(vehicleTwo);
        congestionChargeSystem.vehicleEnteringZone(vehicleOne);
        congestionChargeSystem.vehicleLeavingZone(vehicleOne);
        ZoneBoundaryCrossing z = congestionChargeSystem.getCompleteEventLog().get(0);
        assertThat(z.getVehicle(), is(vehicleOne));
    }

    @Test
    public void checksUnregisteredVehicleException() throws AccountNotRegisteredException, InsufficientCreditException{
        thrown.expect(AccountNotRegisteredException.class);
        RegisteredCustomerAccountsService.getInstance().accountFor(vehicleTwo).deduct(BigDecimal.valueOf(1000000000));
    }

    @Test 
    public void checkHoursBetweenCalculation() throws InvocationTargetException, IllegalAccessException, NoSuchMethodException {
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

    @Test
    public void InsufficientCreditException() throws AccountNotRegisteredException, InsufficientCreditException {
        thrown.expect(InsufficientCreditException.class);
        RegisteredCustomerAccountsService.getInstance().accountFor(vehicleOne).deduct(BigDecimal.valueOf(1000000000));
    }

    @Test
    public void checkOrderingIsFalse(){
        eventLogEntry(new EntryEvent(vehicleOne, getControllableClock(9, 0, 0)));
        eventLogEntry(new EntryEvent(vehicleOne, getControllableClock(10, 0, 0)));
        assertEquals(congestionChargeSystem.getVehicleRegistration().get(vehicleOne).getOrdering(), false);
    }

    @Test
    public void checkOrderingIsTrue(){
        eventLogEntry(new EntryEvent(vehicleOne, getControllableClock(9, 0, 0)));
        eventLogEntry(new ExitEvent(vehicleOne, getControllableClock(10, 0, 0)));
        assertEquals(congestionChargeSystem.getVehicleRegistration().get(vehicleOne).getOrdering(), true);

    }

    @Test
    public void checkOrderingIsFalseWhenStartWithExit(){
        eventLogEntry(new ExitEvent(vehicleOne, getControllableClock(12, 0, 0)));
        eventLogEntry(new EntryEvent(vehicleOne, getControllableClock(13, 0, 0)));
        assertEquals(congestionChargeSystem.getVehicleRegistration().get(vehicleOne).getOrdering(), false);

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
    public void checksTimer() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
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
    public void checkOrderingLastIsEntry(){
        eventLogEntry(new EntryEvent(vehicleOne, getControllableClock(9, 0, 0)));
        eventLogEntry(new ExitEvent(vehicleOne, getControllableClock(11, 0, 0)));
        eventLogEntry(new EntryEvent(vehicleOne, getControllableClock(12, 0, 0)));
        eventLogEntry(new ExitEvent(vehicleOne, getControllableClock(14, 30, 0)));
        eventLogEntry(new EntryEvent(vehicleOne, getControllableClock(16, 0, 0)));
        assertFalse(congestionChargeSystem.getVehicleRegistration().get(vehicleOne).getOrdering());

    }

    @Test
    public void checkOrderingFirstIsExit(){
        eventLogEntry(new ExitEvent(vehicleOne, getControllableClock(11, 0, 0)));
        eventLogEntry(new EntryEvent(vehicleOne, getControllableClock(12, 0, 0)));
        eventLogEntry(new ExitEvent(vehicleOne, getControllableClock(14, 0, 0)));
        eventLogEntry(new EntryEvent(vehicleOne, getControllableClock(16, 0, 0)));
        assertFalse(congestionChargeSystem.getVehicleRegistration().get(vehicleOne).getOrdering());
    }

    @Test
    public void checkMultipleEntryEqualTo14(){
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
    public void checkMultipleEntryLessThatFourHoursBeforeTwo(){
        eventLogEntry(new EntryEvent(vehicleOne, getControllableClock(9, 0, 0)));
        eventLogEntry(new ExitEvent(vehicleOne, getControllableClock(11, 0, 0)));
        eventLogEntry(new EntryEvent(vehicleOne, getControllableClock(12, 0, 0)));
        eventLogEntry(new ExitEvent(vehicleOne, getControllableClock(13, 0, 0)));
        congestionChargeSystem.calculateCharges();
        BigDecimal answer = new BigDecimal("6");
        assertEquals(congestionChargeSystem.getVehicleRegistration().get(vehicleOne).getCharge(), answer);
    }

    @Test
    public void checkMultipleEntryLessThatFourHoursAfterTwo(){
        eventLogEntry(new EntryEvent(vehicleOne, getControllableClock(15, 0, 0)));
        eventLogEntry(new ExitEvent(vehicleOne, getControllableClock(16, 0, 0)));
        eventLogEntry(new EntryEvent(vehicleOne, getControllableClock(18, 0, 0)));
        eventLogEntry(new ExitEvent(vehicleOne, getControllableClock(19, 30, 0)));
        congestionChargeSystem.calculateCharges();
        BigDecimal answer = new BigDecimal("4");
        assertEquals(getVehicleCharge(vehicleOne), answer);
    }

    @Test
    public void checkMultipleEntryMoreThatFourHours(){
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
    public void checkMultipleEntryAfterFourWithMoreThanFourBetween(){
        eventLogEntry(new EntryEvent(vehicleOne, getControllableClock(15, 0, 0)));
        eventLogEntry(new ExitEvent(vehicleOne, getControllableClock(16, 0, 0)));
        eventLogEntry(new EntryEvent(vehicleOne, getControllableClock(23, 0, 0)));
        eventLogEntry(new ExitEvent(vehicleOne, getControllableClock(23, 30, 0)));
        congestionChargeSystem.calculateCharges();
        BigDecimal answer = new BigDecimal("8");
        assertEquals(getVehicleCharge(vehicleOne), answer);
    }

    @Test
    public void checkMultipleEntryBeforeAndAfterTwo(){
        eventLogEntry(new EntryEvent(vehicleOne, getControllableClock(13, 0, 0)));
        eventLogEntry(new ExitEvent(vehicleOne, getControllableClock(14, 0, 0)));
        eventLogEntry(new EntryEvent(vehicleOne, getControllableClock(15, 0, 0)));
        eventLogEntry(new ExitEvent(vehicleOne, getControllableClock(15, 30, 0)));
        congestionChargeSystem.calculateCharges();
        BigDecimal answer = new BigDecimal("6");
        assertEquals(getVehicleCharge(vehicleOne), answer);
    }

    @Test
    public void checkEntryTwoCars(){
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
    public void checkEntryTwoCarsMultipleEntry(){
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
    public void checksIsRegistered(){
        eventLogEntry(new EntryEvent(vehicleOne, getControllableClock(13, 0, 0)));
        eventLogEntry(new ExitEvent(vehicleOne, getControllableClock(14, 0, 0)));
        assertTrue(congestionChargeSystem.isVehicleRegistered(vehicleOne));
        assertFalse(congestionChargeSystem.isVehicleRegistered(vehicleTwo));
    }

    @Test
    public void checkClock() {
        context.checking(new Expectations() {{
            exactly(1).of(clock).now();
        }});
        eventLogEntry(new EntryEvent(vehicleOne, clock));

    }

    @Test
    public void checkClockNotUsed() {
        context.checking(new Expectations() {{
            exactly(0).of(clock).now();
        }});
        eventLogEntry(new EntryEvent(vehicleOne));

    }

}
