package com.trafficmon;

//import static jdk.nashorn.internal.objects.NativeMath.round;
import static junit.framework.TestCase.assertEquals;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.number.OrderingComparison.greaterThan;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Rule;
import org.junit.rules.ExpectedException;
import org.junit.Test;

import java.math.BigDecimal;
import java.math.MathContext;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.math.*;
import java.util.Map;

public class Tests {

    Vehicle vehicleOne = Vehicle.withRegistration("A123 XYZ");
    Vehicle vehicleTwo = Vehicle.withRegistration("B123 XYZ");
    Vehicle vehicleThree = Vehicle.withRegistration("J091 4PY");
    Vehicle vehicleOneCopy = Vehicle.withRegistration("A123 XYZ");
    private ControlableClock cc = new ControlableClock();

    private CongestionChargeSystem system = new CongestionChargeSystem();

    @Rule
    public JUnitRuleMockery context = new JUnitRuleMockery();

    Clock clock = context.mock(Clock.class);

    @Rule
    public ExpectedException thrown = ExpectedException.none();

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
    public void assertExitTimeStamp() throws InterruptedException {
        system.vehicleEnteringZone(vehicleOne);
        Thread.sleep(1000);
        system.vehicleEnteringZone(vehicleTwo);

        ZoneBoundaryCrossing crossingByOne = system.getEventLog().get(0);
        ZoneBoundaryCrossing crossingByTwo = system.getEventLog().get(1);
        assertThat(crossingByTwo.timestamp(), greaterThan(crossingByOne.timestamp()));
    }

    @Test
    public void assertEnteringTime() throws InterruptedException {
        system.vehicleEnteringZone(vehicleOne);
        LocalTime timestamp = system.getEventLog().get(0).timestamp();
        system.vehicleLeavingZone(vehicleOne);
        Thread.sleep(1000);
        system.vehicleEnteringZone(vehicleOne);
        ZoneBoundaryCrossing crossing = system.getEventLog().get(2);
        assertThat(crossing.timestamp(), greaterThan(timestamp));

    }

    @Test
    public void isLeavingRegisteredInEventLog(){
        system.vehicleLeavingZone(vehicleOne);
        assertThat(system.getEventLog().size(), is(0));
        system.vehicleEnteringZone(vehicleOne);
        system.vehicleLeavingZone(vehicleOne);
        assertThat(system.getEventLog().size(), is(2));
    }

    @Test
    public void testEventLogZeroValue(){
        system.vehicleLeavingZone(vehicleTwo);
        system.vehicleEnteringZone(vehicleOne);
        system.vehicleLeavingZone(vehicleOne);
        ZoneBoundaryCrossing z = system.getEventLog().get(0);
        assertThat(z.getVehicle(), is(vehicleOne));
    }

    @Test
    public void checksUnregisteredVehicleException() throws AccountNotRegisteredException, InsufficientCreditException{
        thrown.expect(AccountNotRegisteredException.class);
        //system.vehicleEnteringZone(vehicleTwo);
        RegisteredCustomerAccountsService.getInstance().accountFor(vehicleTwo).deduct(BigDecimal.valueOf(1000000000));
    }

    @Test public void checkTheMaths(){
        LocalTime startTimeMs= LocalTime.of(9,10,50);
        LocalTime endTimeMs= LocalTime.of(10,11,50);
        ChargeTest g = new ChargeTest();
        assertEquals(61/60.0, g.getter(startTimeMs, endTimeMs));
    }


    @Test
    public void InsufficientCreditException() throws AccountNotRegisteredException, InsufficientCreditException {
        thrown.expect(InsufficientCreditException.class);
        RegisteredCustomerAccountsService.getInstance().accountFor(vehicleOne).deduct(BigDecimal.valueOf(1000000000));
    }

    @Test
    public void checkOrderingIsFalse()  throws InterruptedException{
        system.vehicleEnteringZone(vehicleOne);
        Thread.sleep(1000);
        system.vehicleEnteringZone(vehicleOne);
        List<ZoneBoundaryCrossing> crossings = new ArrayList<>();
        crossings.addAll(system.getEventLog());
        assertEquals(system.getOrdering(crossings), false);
    }

    @Test
    public void checkOrderingIsTrue()  throws InterruptedException{
        system.vehicleEnteringZone(vehicleOne);
        Thread.sleep(1000);
        system.vehicleLeavingZone(vehicleOne);
        List<ZoneBoundaryCrossing> crossings = new ArrayList<>();
        crossings.addAll(system.getEventLog());
        assertEquals(system.getOrdering(crossings), true);
    }

    @Test
    public void checkOrderingIsFalseWhenStartWithExit()  throws InterruptedException{
        system.vehicleLeavingZone(vehicleOne);
        Thread.sleep(1000);
        system.vehicleEnteringZone(vehicleOne);
        List<ZoneBoundaryCrossing> crossings = new ArrayList<>();
        crossings.addAll(system.getEventLog());
        assertEquals(system.getOrdering(crossings), false);
    }

    @Test
    public void calculatesChargeForEntryBeforeTwoLessThanFourHours(){
        cc.currentTimeIs(9, 0 , 0);
        system.getEventLog().add(new EntryEvent(vehicleOne, cc));
        cc.currentTimeIs(11, 0 , 0);
        system.getEventLog().add(new ExitEvent(vehicleOne, cc));
        system.calculateCharges();
        BigDecimal bd = (BigDecimal) system.charge2().get(vehicleOne);
        BigDecimal v = bd.round(new MathContext(1));
        BigDecimal answer = new BigDecimal("6");
        assertEquals(v, answer);
    }

    @Test
    public void calculatesChargeForLongerThanFourHours(){
        cc.currentTimeIs(9, 0 , 0);
        system.getEventLog().add(new EntryEvent(vehicleOne, cc));
        cc.currentTimeIs(14, 30 , 0);
        system.getEventLog().add(new EntryEvent(vehicleThree, cc));
        cc.currentTimeIs(18, 0 , 0);
        system.getEventLog().add(new ExitEvent(vehicleOne, cc));
        cc.currentTimeIs(22, 0 , 0);
        system.getEventLog().add(new ExitEvent(vehicleThree, cc));
        system.calculateCharges();
        BigDecimal bd1 = (BigDecimal) system.charge2().get(vehicleOne);
        BigDecimal v1 = bd1.round(new MathContext(2));
        BigDecimal bd2 = (BigDecimal) system.charge2().get(vehicleThree);
        BigDecimal v2 = bd2.round(new MathContext(2));
        assertEquals(v1, v2);
    }

    @Test
    public void calculatesChargeForEntryAfterTwoLessThanFourHours(){
        cc.currentTimeIs(15, 0 , 0);
        system.getEventLog().add(new EntryEvent(vehicleOne, cc));
        cc.currentTimeIs(16, 0 , 0);
        system.getEventLog().add(new ExitEvent(vehicleOne, cc));
        system.calculateCharges();
        BigDecimal bd = (BigDecimal) system.charge2().get(vehicleOne);
        BigDecimal v = bd.round(new MathContext(1));
        BigDecimal answer = new BigDecimal("4");
        assertEquals(v, answer);
    }

    @Test
    public void checksTimer(){
        List<ZoneBoundaryCrossing> crossings = new ArrayList<>();
        cc.currentTimeIs(9, 0 , 0);
        crossings.add(0, new EntryEvent(vehicleOne, cc));
        cc.currentTimeIs(11, 0 , 0);
        crossings.add(1, new ExitEvent(vehicleOne, cc));
        cc.currentTimeIs(12, 0 , 0);
        crossings.add(2, new EntryEvent(vehicleOne, cc));
        cc.currentTimeIs(14, 30 , 0);
        crossings.add(3, new ExitEvent(vehicleOne, cc));
        assertEquals(new ChargeTest().timer(crossings), 4.5);
    }

    @Test
    public void calculatesChargesWithLeaving(){
        cc.currentTimeIs(9, 0 , 0);
        system.getEventLog().add(new EntryEvent(vehicleOne, cc));
        cc.currentTimeIs(11, 0 , 0);
        system.getEventLog().add(new ExitEvent(vehicleOne, cc));
        cc.currentTimeIs(12, 0 , 0);
        system.getEventLog().add(new EntryEvent(vehicleOne, cc));
        cc.currentTimeIs(13, 0 , 0);
        system.getEventLog().add(new ExitEvent(vehicleOne, cc));
        cc.currentTimeIs(19, 0 , 0);
        system.getEventLog().add(new EntryEvent(vehicleOne, cc));
        cc.currentTimeIs(19, 30 , 0);
        system.getEventLog().add(new ExitEvent(vehicleOne, cc));
        system.calculateCharges();
        BigDecimal bd = (BigDecimal) system.charge2().get(vehicleOne);
        BigDecimal v = bd.round(new MathContext(2));
        BigDecimal answer = new BigDecimal("10");
        assertEquals(v, answer);
    }

    @Test
    public void checkOrderingLastIsEntry(){
        List<ZoneBoundaryCrossing> crossings = new ArrayList<>();
        cc.currentTimeIs(9, 0 , 0);
        crossings.add(0, new EntryEvent(vehicleOne, cc));
        cc.currentTimeIs(11, 0 , 0);
        crossings.add(1, new ExitEvent(vehicleOne, cc));
        cc.currentTimeIs(12, 0 , 0);
        crossings.add(2, new EntryEvent(vehicleOne, cc));
        cc.currentTimeIs(14, 30 , 0);
        crossings.add(3, new ExitEvent(vehicleOne, cc));
        cc.currentTimeIs(16, 0 , 0);
        crossings.add(4, new EntryEvent(vehicleOne, cc));
        assertFalse((system.getOrdering(crossings)));
    }

    @Test
    public void checkOrderingFirstIsExit(){
        List<ZoneBoundaryCrossing> crossings = new ArrayList<>();
        cc.currentTimeIs(11, 0 , 0);
        crossings.add(0, new ExitEvent(vehicleOne, cc));
        cc.currentTimeIs(12, 0 , 0);
        crossings.add(1, new EntryEvent(vehicleOne, cc));
        cc.currentTimeIs(14, 0 , 0);
        crossings.add(2, new ExitEvent(vehicleOne, cc));
        cc.currentTimeIs(16, 0 , 0);
        crossings.add(3, new EntryEvent(vehicleOne, cc));
        assertFalse((system.getOrdering(crossings)));
    }

    @Test
    public void checkMultipleEntryEqualTo14(){
        cc.currentTimeIs(9, 0 , 0);
        system.getEventLog().add(new EntryEvent(vehicleOne, cc));
        cc.currentTimeIs(11, 0 , 0);
        system.getEventLog().add(new ExitEvent(vehicleOne, cc));
        cc.currentTimeIs(16, 0 , 0);
        system.getEventLog().add(new EntryEvent(vehicleOne, cc));
        cc.currentTimeIs(17, 0 , 0);
        system.getEventLog().add(new ExitEvent(vehicleOne, cc));
        cc.currentTimeIs(22, 0 , 0);
        system.getEventLog().add(new EntryEvent(vehicleOne, cc));
        cc.currentTimeIs(22, 30 , 0);
        system.getEventLog().add(new ExitEvent(vehicleOne, cc));
        system.calculateCharges();
        BigDecimal bd = (BigDecimal) system.charge2().get(vehicleOne);
        BigDecimal v = bd.round(new MathContext(2));
        BigDecimal answer = new BigDecimal("14");
        assertEquals(v, answer);
    }

    @Test
    public void checkMultipleEntryLessThatFourHoursBeforeTwo(){
        cc.currentTimeIs(9, 0 , 0);
        system.getEventLog().add(new EntryEvent(vehicleOne, cc));
        cc.currentTimeIs(11, 0 , 0);
        system.getEventLog().add(new ExitEvent(vehicleOne, cc));
        cc.currentTimeIs(12, 0 , 0);
        system.getEventLog().add(new EntryEvent(vehicleOne, cc));
        cc.currentTimeIs(13, 0 , 0);
        system.getEventLog().add(new ExitEvent(vehicleOne, cc));
        system.calculateCharges();
        BigDecimal bd = (BigDecimal) system.charge2().get(vehicleOne);
        BigDecimal v = bd.round(new MathContext(2));
        BigDecimal answer = new BigDecimal("6");
        assertEquals(v, answer);
    }

    @Test
    public void checkMultipleEntryLessThatFourHoursAfterTwo(){
        cc.currentTimeIs(15, 0 , 0);
        system.getEventLog().add(new EntryEvent(vehicleOne, cc));
        cc.currentTimeIs(16, 0 , 0);
        system.getEventLog().add(new ExitEvent(vehicleOne, cc));
        cc.currentTimeIs(18, 0 , 0);
        system.getEventLog().add(new EntryEvent(vehicleOne, cc));
        cc.currentTimeIs(19, 30 , 0);
        system.getEventLog().add(new ExitEvent(vehicleOne, cc));
        system.calculateCharges();
        BigDecimal bd = (BigDecimal) system.charge2().get(vehicleOne);
        BigDecimal v = bd.round(new MathContext(2));
        BigDecimal answer = new BigDecimal("4");
        assertEquals(v, answer);
    }

    @Test
    public void checkMultipleEntryMoreThatFourHours(){
        cc.currentTimeIs(9, 0 , 0);
        system.getEventLog().add(new EntryEvent(vehicleOne, cc));
        cc.currentTimeIs(11, 0 , 0);
        system.getEventLog().add(new ExitEvent(vehicleOne, cc));
        cc.currentTimeIs(15, 0 , 0);
        system.getEventLog().add(new EntryEvent(vehicleOne, cc));
        cc.currentTimeIs(16, 0 , 0);
        system.getEventLog().add(new ExitEvent(vehicleOne, cc));
        cc.currentTimeIs(18, 0 , 0);
        system.getEventLog().add(new EntryEvent(vehicleOne, cc));
        cc.currentTimeIs(19, 30 , 0);
        system.getEventLog().add(new ExitEvent(vehicleOne, cc));
        system.calculateCharges();
        BigDecimal bd = (BigDecimal) system.charge2().get(vehicleOne);
        BigDecimal v = bd.round(new MathContext(2));
        BigDecimal answer = new BigDecimal("12");
        assertEquals(v, answer);
    }

    @Test
    public void checkMultipleEntryAfterFourWithMoreThanFourBetween(){
        cc.currentTimeIs(15, 0 , 0);
        system.getEventLog().add(new EntryEvent(vehicleOne, cc));
        cc.currentTimeIs(16, 0 , 0);
        system.getEventLog().add(new ExitEvent(vehicleOne, cc));
        cc.currentTimeIs(23, 0 , 0);
        system.getEventLog().add(new EntryEvent(vehicleOne, cc));
        cc.currentTimeIs(23, 30 , 0);
        system.getEventLog().add(new ExitEvent(vehicleOne, cc));
        system.calculateCharges();
        BigDecimal bd = (BigDecimal) system.charge2().get(vehicleOne);
        BigDecimal v = bd.round(new MathContext(2));
        BigDecimal answer = new BigDecimal("8");
        assertEquals(v, answer);
    }

    @Test
    public void checkMultipleEntryBeforeAndAfterTwo(){
        cc.currentTimeIs(13, 0 , 0);
        system.getEventLog().add(new EntryEvent(vehicleOne, cc));
        cc.currentTimeIs(14, 0 , 0);
        system.getEventLog().add(new ExitEvent(vehicleOne, cc));
        cc.currentTimeIs(15, 0 , 0);
        system.getEventLog().add(new EntryEvent(vehicleOne, cc));
        cc.currentTimeIs(15, 30 , 0);
        system.getEventLog().add(new ExitEvent(vehicleOne, cc));
        system.calculateCharges();
        BigDecimal bd = (BigDecimal) system.charge2().get(vehicleOne);
        BigDecimal v = bd.round(new MathContext(2));
        BigDecimal answer = new BigDecimal("6");
        assertEquals(v, answer);
    }

    @Test
    public void checkEntryTwoCars(){
        cc.currentTimeIs(12, 0 , 0);
        system.getEventLog().add(new EntryEvent(vehicleOne, cc));
        cc.currentTimeIs(13, 0 , 0);
        system.getEventLog().add(new EntryEvent(vehicleTwo, cc));
        cc.currentTimeIs(14, 0 , 0);
        system.getEventLog().add(new ExitEvent(vehicleOne, cc));
        cc.currentTimeIs(18, 30 , 0);
        system.getEventLog().add(new ExitEvent(vehicleTwo, cc));
        system.calculateCharges();
        BigDecimal bd1 = (BigDecimal) system.charge2().get(vehicleOne);
        BigDecimal bd2 = (BigDecimal) system.charge2().get(vehicleTwo);
        BigDecimal v1 = bd1.round(new MathContext(2));
        BigDecimal v2 = bd2.round(new MathContext(2));
        BigDecimal answer1 = new BigDecimal("6");
        BigDecimal answer2 = new BigDecimal("12");
        assertEquals(v1, answer1);
        assertEquals(v2, answer2);
    }

    @Test
    public void checkEntryTwoCarsMultipleEntry(){
        cc.currentTimeIs(13, 0 , 0);
        system.getEventLog().add(new EntryEvent(vehicleOne, cc));
        cc.currentTimeIs(14, 0 , 0);
        system.getEventLog().add(new ExitEvent(vehicleOne, cc));
        cc.currentTimeIs(15, 0 , 0);
        system.getEventLog().add(new EntryEvent(vehicleOne, cc));
        cc.currentTimeIs(15, 30 , 0);
        system.getEventLog().add(new ExitEvent(vehicleOne, cc));

        cc.currentTimeIs(15, 0 , 0);
        system.getEventLog().add(new EntryEvent(vehicleTwo, cc));
        cc.currentTimeIs(16, 0 , 0);
        system.getEventLog().add(new ExitEvent(vehicleTwo, cc));
        cc.currentTimeIs(23, 0 , 0);
        system.getEventLog().add(new EntryEvent(vehicleTwo, cc));
        cc.currentTimeIs(23, 30 , 0);
        system.getEventLog().add(new ExitEvent(vehicleTwo, cc));

        system.calculateCharges();
        BigDecimal bd1 = (BigDecimal) system.charge2().get(vehicleOne);
        BigDecimal bd2 = (BigDecimal) system.charge2().get(vehicleTwo);
        BigDecimal v1 = bd1.round(new MathContext(2));
        BigDecimal v2 = bd2.round(new MathContext(2));
        BigDecimal answer1 = new BigDecimal("6");
        BigDecimal answer2 = new BigDecimal("8");
        assertEquals(v1, answer1);
        assertEquals(v2, answer2);
    }


    private class ControlableClock implements Clock {
        private LocalTime now;

        @Override
        public LocalTime now() {
            return now;
        }

        public void currentTimeIs(int hour, int min, int sec) {
            now = LocalTime.of(hour,min,sec);
        }
    }
}
