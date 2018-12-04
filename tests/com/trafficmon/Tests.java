package com.trafficmon;

//import static jdk.nashorn.internal.objects.NativeMath.round;
import static junit.framework.TestCase.assertEquals;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.number.OrderingComparison.greaterThan;
import static org.junit.Assert.assertNotEquals;

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

    CongestionChargeSystem system = new CongestionChargeSystem();

    @Rule
    public JUnitRuleMockery context = new JUnitRuleMockery();

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
        system.vehicleEnteringZone(vehicleTwo);
        RegisteredCustomerAccountsService.getInstance().accountFor(vehicleTwo).deduct(BigDecimal.valueOf(1000000000));
    }

    @Test public void checkTheMaths(){
        LocalTime startTimeMs= LocalTime.of(9,10,50);
        LocalTime endTimeMs= LocalTime.of(9,11,50);
        CongestionChargeSystem g = new CongestionChargeSystem();
        assertEquals(1, g.getter(startTimeMs, endTimeMs));
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
        for (ZoneBoundaryCrossing crossing : system.getEventLog()){
            crossings.add(crossing);
        }
        assertEquals(system.getOrdering(crossings), false);
    }

    @Test
    public void checkOrderingIsTrue()  throws InterruptedException{
        system.vehicleEnteringZone(vehicleOne);
        Thread.sleep(1000);
        system.vehicleLeavingZone(vehicleOne);
        List<ZoneBoundaryCrossing> crossings = new ArrayList<>();
        for (ZoneBoundaryCrossing crossing : system.getEventLog()){
            crossings.add(crossing);
        }
        assertEquals(system.getOrdering(crossings), true);
    }

    @Test
    public void calculatesChargeForEntryBeforeTwoLessThanFourHours(){
        system.getEventLog().add(new EntryEvent(vehicleOne, LocalTime.of(9,0,0)));
        system.getEventLog().add(new ExitEvent(vehicleOne, LocalTime.of(11,0,0)));
        system.calculateCharges();
        BigDecimal bd = (BigDecimal) system.charge2().get(vehicleOne);
        BigDecimal v = bd.round(new MathContext(1));
        BigDecimal answer = new BigDecimal("6");
        assertEquals(v, answer);
    }

    @Test
    public void calculatesChargeForLongerThanFourHours(){
        system.getEventLog().add(new EntryEvent(vehicleOne, LocalTime.of(9,0,0)));
        system.getEventLog().add(new EntryEvent(vehicleThree, LocalTime.of(14, 30,0)));
        system.getEventLog().add(new ExitEvent(vehicleOne, LocalTime.of(18,0,0)));
        system.getEventLog().add(new ExitEvent(vehicleThree, LocalTime.of(22,0,0)));
        system.calculateCharges();
        BigDecimal bd1 = (BigDecimal) system.charge2().get(vehicleOne);
        BigDecimal v1 = bd1.round(new MathContext(2));
        BigDecimal bd2 = (BigDecimal) system.charge2().get(vehicleThree);
        BigDecimal v2 = bd2.round(new MathContext(2));
        assertEquals(v1, v2);
    }
}
