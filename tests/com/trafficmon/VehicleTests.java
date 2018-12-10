package com.trafficmon;

import org.junit.Test;

import static junit.framework.TestCase.assertEquals;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertThat;

public class VehicleTests extends TestHelper {
    @Test
    public void testsGetRegistration(){
        assertEquals(vehicleOne.getRegistration(), "A123 XYZ");
        assertNotEquals(vehicleTwo.getRegistration(), "A123 XYZ");
    }

    @Test
    public void testsVehicleToSting(){
        assertEquals(vehicleOne.toString(), "Vehicle [A123 XYZ]");
    }

    @Test
    public void testsVehicleEquals(){
        assertThat(vehicleOne.equals(vehicleOneCopy), is(true));
        assertThat(vehicleOne.equals(vehicleThree), is(false));
    }

    @Test
    public void testsVehicleHashCode(){
        assertEquals(vehicleOne.hashCode(), vehicleOneCopy.hashCode());
        assertNotEquals(vehicleOne.hashCode(), vehicleThree.hashCode());
        assertNotEquals(vehicleOne.hashCode(), 0);
        assertEquals(Vehicle.withRegistration(null).hashCode(), 0);
    }
}
