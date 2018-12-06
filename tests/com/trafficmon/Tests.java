package com.trafficmon;

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

public class Tests {

    Vehicle vehicleOne = Vehicle.withRegistration("A123 XYZ");
    Vehicle vehicleTwo = Vehicle.withRegistration("B123 XYZ");
    Vehicle vehicleThree = Vehicle.withRegistration("J091 4PY");
    Vehicle vehicleOneCopy = Vehicle.withRegistration("A123 XYZ");
    private ControllableClock cc = new ControllableClock();

    private CongestionChargeSystem system = new CongestionChargeSystem();

    @Rule
    public JUnitRuleMockery context = new JUnitRuleMockery();

    Clock clock = context.mock(Clock.class);

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private void eventLogEntry(ZoneBoundaryCrossing crossing){
        system.getEventLog().add(crossing);
    }

    private ControllableClock getCC(int hr, int min, int sec){
        cc.currentTimeIs(hr, min, sec);
        return cc;
    }

    private BigDecimal bd(Vehicle vehicle){
        BigDecimal ans = (BigDecimal) system.getChargeMap().get(vehicle);
        return ans.round(new MathContext(2));
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
        eventLogEntry(new EntryEvent(vehicleOne, getCC(9, 0, 0)));
        eventLogEntry(new ExitEvent(vehicleOne, getCC(10, 0, 0)));
        ZoneBoundaryCrossing crossingByOne = system.getEventLog().get(0);
        ZoneBoundaryCrossing crossingByTwo = system.getEventLog().get(1);
        assertThat(crossingByTwo.timestamp(), greaterThan(crossingByOne.timestamp()));
    }

    @Test
    public void assertEnteringTime(){
        eventLogEntry(new EntryEvent(vehicleOne, getCC(9, 0, 0)));
        eventLogEntry(new ExitEvent(vehicleOne, getCC(10, 0, 0)));
        LocalTime timestamp = system.getEventLog().get(0).timestamp();
        eventLogEntry(new EntryEvent(vehicleOne, getCC(12, 0, 0)));
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
        RegisteredCustomerAccountsService.getInstance().accountFor(vehicleTwo).deduct(BigDecimal.valueOf(1000000000));
    }

    @Test public void checkTheMaths(){
        LocalTime startTimeMs= LocalTime.of(9,10,50);
        LocalTime endTimeMs= LocalTime.of(10,11,50);
        ChargeCalculator g = new ChargeCalculator();
        assertEquals(61/60.0, g.getter(startTimeMs, endTimeMs));
    }


    @Test
    public void InsufficientCreditException() throws AccountNotRegisteredException, InsufficientCreditException {
        thrown.expect(InsufficientCreditException.class);
        RegisteredCustomerAccountsService.getInstance().accountFor(vehicleOne).deduct(BigDecimal.valueOf(1000000000));
    }

    @Test
    public void checkOrderingIsFalse(){
        eventLogEntry(new EntryEvent(vehicleOne, getCC(9, 0, 0)));
        eventLogEntry(new EntryEvent(vehicleOne, getCC(10, 0, 0)));
        List<ZoneBoundaryCrossing> crossings = new ArrayList<>(system.getEventLog());
        assertEquals(system.getOrdering(crossings), false);
    }

    @Test
    public void checkOrderingIsTrue(){
        eventLogEntry(new EntryEvent(vehicleOne, getCC(9, 0, 0)));
        eventLogEntry(new ExitEvent(vehicleOne, getCC(10, 0, 0)));
        assertEquals(system.getOrdering(system.getEventLog()), true);
    }

    @Test
    public void checkOrderingIsFalseWhenStartWithExit(){
        eventLogEntry(new ExitEvent(vehicleOne, getCC(12, 0, 0)));
        eventLogEntry(new EntryEvent(vehicleOne, getCC(13, 0, 0)));
        assertEquals(system.getOrdering(system.getEventLog()), false);
    }

    @Test
    public void calculatesChargeForEntryBeforeTwoLessThanFourHours(){
        eventLogEntry(new EntryEvent(vehicleOne, getCC(9, 0, 0)));
        eventLogEntry(new ExitEvent(vehicleOne, getCC(11, 0, 0)));
        system.calculateCharges();
        BigDecimal answer = new BigDecimal("6");
        assertEquals(bd(vehicleOne), answer);
    }

    @Test
    public void calculatesChargeForLongerThanFourHours(){
        eventLogEntry(new EntryEvent(vehicleOne, getCC(9, 0, 0)));
        eventLogEntry(new EntryEvent(vehicleThree, getCC(14, 30, 0)));
        eventLogEntry(new ExitEvent(vehicleOne, getCC(18, 0, 0)));
        eventLogEntry(new ExitEvent(vehicleThree, getCC(22, 0, 0)));
        system.calculateCharges();
        assertEquals(bd(vehicleOne), bd(vehicleThree));
    }

    @Test
    public void calculatesChargeForEntryAfterTwoLessThanFourHours(){
        eventLogEntry(new EntryEvent(vehicleOne, getCC(15, 0, 0)));
        eventLogEntry(new ExitEvent(vehicleOne, getCC(16, 0, 0)));
        system.calculateCharges();
        BigDecimal answer = new BigDecimal("4");
        assertEquals(bd(vehicleOne), answer);
    }

    @Test
    public void checksTimer(){
        eventLogEntry(new EntryEvent(vehicleOne, getCC(9, 0, 0)));
        eventLogEntry(new ExitEvent(vehicleOne, getCC(11, 0, 0)));
        eventLogEntry(new EntryEvent(vehicleOne, getCC(12, 0, 0)));
        eventLogEntry(new ExitEvent(vehicleOne, getCC(14, 30, 0)));
        assertEquals(new ChargeCalculator().timer(system.getEventLog()), 4.5);
    }

    @Test
    public void calculatesChargesWithLeaving(){
        eventLogEntry(new EntryEvent(vehicleOne, getCC(9, 0, 0)));
        eventLogEntry(new ExitEvent(vehicleOne, getCC(11, 0, 0)));
        eventLogEntry(new EntryEvent(vehicleOne, getCC(12, 0, 0)));
        eventLogEntry(new ExitEvent(vehicleOne, getCC(13, 0, 0)));
        eventLogEntry(new EntryEvent(vehicleOne, getCC(19, 0, 0)));
        eventLogEntry(new ExitEvent(vehicleOne, getCC(19, 30, 0)));
        system.calculateCharges();
        BigDecimal answer = new BigDecimal("10");
        assertEquals(bd(vehicleOne), answer);
    }

    @Test
    public void checkOrderingLastIsEntry(){
        eventLogEntry(new EntryEvent(vehicleOne, getCC(9, 0, 0)));
        eventLogEntry(new ExitEvent(vehicleOne, getCC(11, 0, 0)));
        eventLogEntry(new EntryEvent(vehicleOne, getCC(12, 0, 0)));
        eventLogEntry(new ExitEvent(vehicleOne, getCC(14, 30, 0)));
        eventLogEntry(new EntryEvent(vehicleOne, getCC(16, 0, 0)));
        assertFalse(system.getOrdering(system.getEventLog()));
    }

    @Test
    public void checkOrderingFirstIsExit(){
        eventLogEntry(new ExitEvent(vehicleOne, getCC(11, 0, 0)));
        eventLogEntry(new EntryEvent(vehicleOne, getCC(12, 0, 0)));
        eventLogEntry(new ExitEvent(vehicleOne, getCC(14, 0, 0)));
        eventLogEntry(new EntryEvent(vehicleOne, getCC(16, 0, 0)));
        assertFalse((system.getOrdering(system.getEventLog())));
    }

    @Test
    public void checkMultipleEntryEqualTo14(){
        eventLogEntry(new EntryEvent(vehicleOne, getCC(9, 0, 0)));
        eventLogEntry(new ExitEvent(vehicleOne, getCC(11, 0, 0)));
        eventLogEntry(new EntryEvent(vehicleOne, getCC(16, 0, 0)));
        eventLogEntry(new ExitEvent(vehicleOne, getCC(17, 0, 0)));
        eventLogEntry(new EntryEvent(vehicleOne, getCC(22, 0, 0)));
        eventLogEntry(new ExitEvent(vehicleOne, getCC(22, 30, 0)));
        system.calculateCharges();
        BigDecimal answer = new BigDecimal("14");
        assertEquals(bd(vehicleOne), answer);
    }

    @Test
    public void checkMultipleEntryLessThatFourHoursBeforeTwo(){
        eventLogEntry(new EntryEvent(vehicleOne, getCC(9, 0, 0)));
        eventLogEntry(new ExitEvent(vehicleOne, getCC(11, 0, 0)));
        eventLogEntry(new EntryEvent(vehicleOne, getCC(12, 0, 0)));
        eventLogEntry(new ExitEvent(vehicleOne, getCC(13, 0, 0)));
        system.calculateCharges();
        BigDecimal answer = new BigDecimal("6");
        assertEquals(bd(vehicleOne), answer);
    }

    @Test
    public void checkMultipleEntryLessThatFourHoursAfterTwo(){
        eventLogEntry(new EntryEvent(vehicleOne, getCC(15, 0, 0)));
        eventLogEntry(new ExitEvent(vehicleOne, getCC(16, 0, 0)));
        eventLogEntry(new EntryEvent(vehicleOne, getCC(18, 0, 0)));
        eventLogEntry(new ExitEvent(vehicleOne, getCC(19, 30, 0)));
        system.calculateCharges();
        BigDecimal answer = new BigDecimal("4");
        assertEquals(bd(vehicleOne), answer);
    }

    @Test
    public void checkMultipleEntryMoreThatFourHours(){
        eventLogEntry(new EntryEvent(vehicleOne, getCC(9, 0, 0)));
        eventLogEntry(new ExitEvent(vehicleOne, getCC(11, 0, 0)));
        eventLogEntry(new EntryEvent(vehicleOne, getCC(15, 0, 0)));
        eventLogEntry(new ExitEvent(vehicleOne, getCC(16, 0, 0)));
        eventLogEntry(new EntryEvent(vehicleOne, getCC(18, 0, 0)));
        eventLogEntry(new ExitEvent(vehicleOne, getCC(19, 30, 0)));
        system.calculateCharges();
        BigDecimal answer = new BigDecimal("12");
        assertEquals(bd(vehicleOne), answer);
    }

    @Test
    public void checkMultipleEntryAfterFourWithMoreThanFourBetween(){
        eventLogEntry(new EntryEvent(vehicleOne, getCC(15, 0, 0)));
        eventLogEntry(new ExitEvent(vehicleOne, getCC(16, 0, 0)));
        eventLogEntry(new EntryEvent(vehicleOne, getCC(23, 0, 0)));
        eventLogEntry(new ExitEvent(vehicleOne, getCC(23, 30, 0)));
        system.calculateCharges();
        BigDecimal answer = new BigDecimal("8");
        assertEquals(bd(vehicleOne), answer);
    }

    @Test
    public void checkMultipleEntryBeforeAndAfterTwo(){
        eventLogEntry(new EntryEvent(vehicleOne, getCC(13, 0, 0)));
        eventLogEntry(new ExitEvent(vehicleOne, getCC(14, 0, 0)));
        eventLogEntry(new EntryEvent(vehicleOne, getCC(15, 0, 0)));
        eventLogEntry(new ExitEvent(vehicleOne, getCC(15, 30, 0)));
        system.calculateCharges();
        BigDecimal answer = new BigDecimal("6");
        assertEquals(bd(vehicleOne), answer);
    }

    @Test
    public void checkEntryTwoCars(){
        eventLogEntry(new EntryEvent(vehicleOne, getCC(12, 0, 0)));
        eventLogEntry(new EntryEvent(vehicleTwo, getCC(13, 0, 0)));
        eventLogEntry(new ExitEvent(vehicleOne, getCC(14, 0, 0)));
        eventLogEntry(new ExitEvent(vehicleTwo, getCC(18, 30, 0)));

        system.calculateCharges();
        BigDecimal answer1 = new BigDecimal("6");
        BigDecimal answer2 = new BigDecimal("12");
        assertEquals(bd(vehicleOne), answer1);
        assertEquals(bd(vehicleTwo), answer2);
    }

    @Test
    public void checkEntryTwoCarsMultipleEntry(){
        eventLogEntry(new EntryEvent(vehicleOne, getCC(13, 0, 0)));
        eventLogEntry(new ExitEvent(vehicleOne, getCC(14, 0, 0)));
        eventLogEntry(new EntryEvent(vehicleOne, getCC(15, 0, 0)));
        eventLogEntry(new ExitEvent(vehicleOne, getCC(15, 30, 0)));

        eventLogEntry(new EntryEvent(vehicleTwo, getCC(15, 0, 0)));
        eventLogEntry(new ExitEvent(vehicleTwo, getCC(16, 0, 0)));
        eventLogEntry(new EntryEvent(vehicleTwo, getCC(23, 0, 0)));
        eventLogEntry(new ExitEvent(vehicleTwo, getCC(23, 30, 0)));

        system.calculateCharges();

        BigDecimal answer1 = new BigDecimal("6");
        BigDecimal answer2 = new BigDecimal("8");
        assertEquals(bd(vehicleOne), answer1);
        assertEquals(bd(vehicleTwo), answer2);
    }

    @Test
    public void checksIsRegistered(){
        eventLogEntry(new EntryEvent(vehicleOne, getCC(13, 0, 0)));
        eventLogEntry(new ExitEvent(vehicleOne, getCC(14, 0, 0)));
        assertTrue(system.isRegistered(vehicleOne));
        assertFalse(system.isRegistered(vehicleTwo));
    }

    private class ControllableClock implements Clock {
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
