package com.trafficmon;

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
import java.util.ArrayList;
import java.util.List;

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
        long timestamp = system.getEventLog().get(0).timestamp();
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
        long startTimeMs= 15;
        long endTimeMs= 20 ;
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

    /*
    //this test will check the charge for vehicle
    //entering before 2pm and staying for < 4 hrs
    @Test
    public void checksIfCalculationIsCorrectBeforeTwo() throws InterruptedException{
        //we will have to record time when entering and leaving the zone
        //important - time of day
        //for this test entry time should be less than 2 pm
        system.vehicleEnteringZone(vehicle);
        system.vehicleEnteringZone(vehicleThree);
        Thread.sleep(1000*60*60); //sleep for 1 hour
        system.vehicleLeavingZone(vehicle);
        Thread.sleep(1000*60*60*8);
        //possibly create a list for the cars here? or need to get crossings for a vehicle
        assertThat(system.getCharge(system.getEventLog()), is(6));
        //how to assert 12???
        //how exactly do we get charge for a single vehicle??
        //i can split into two tests?
        //i kinda think it's better to test both within one test bc also checks correctness of methods
    }

    //alternatively use this test
    //checks charge for 5 hrs stay
    @Test
    public void checksIfCalculationIsCorrectFourHrs() throws InterruptedException{
        //for this test entry time is any time
        system.vehicleEnteringZone(vehicle);
        Thread.sleep(1000*60*60*5); //sleep for 5 hour
        system.vehicleLeavingZone(vehicle);
        assertThat(system.getCharge(system.getEventLog()), is(12));
    }

    //checks when leaving and returning within 4 hrs
    @Test
    public void checksLeavingAndEnteringWithinFourHours() throws InterruptedException{
        system.vehicleEnteringZone(vehicle);
        Thread.sleep(1000*60);
        system.vehicleLeavingZone(vehicle);
        Thread.sleep(1000*60);
        system.vehicleEnteringZone(vehicle);
        Thread.sleep(1000*60);
        system.vehicleLeavingZone(vehicle);
        assertThat(system.getCharge(system.getEventLog()), is(4)); //or 6
    }

    //checks when entering after 2pm
    //same as first, need to pass time
    @Test
    public void checksIfCalculationIsCorrectAfterTwo() throws InterruptedException{
        //we will have to record time when entering and leaving the zone
        //important - time of day
        //for this test entry time should be more than 2 pm
        system.vehicleEnteringZone(vehicle);
        system.vehicleEnteringZone(vehicleThree);
        Thread.sleep(1000*60*60); //sleep for 1 hour
        system.vehicleLeavingZone(vehicle);
        Thread.sleep(1000*60*60*8);
        //possibly create a list for the cars here? or need to get crossings for a vehicle
        assertThat(system.getCharge(system.getEventLog()), is(4));
        //assertThat 12
    }
    */
}
