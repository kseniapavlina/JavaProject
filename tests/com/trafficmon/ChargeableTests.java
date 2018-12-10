package com.trafficmon;

import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Rule;
import org.junit.Test;

import java.math.BigDecimal;

public class ChargeableTests extends TestHelper {
    @Rule
    public JUnitRuleMockery context = new JUnitRuleMockery();

    private final Chargeable chargeable = context.mock(Chargeable.class);
    private final PenaltiesService penaltiesService = context.mock(PenaltiesService.class);

    private BigDecimal charge = new BigDecimal(0);

    @Test
    public void testsChargeable() {
        CongestionChargeSystem ccs = new CongestionChargeSystem(chargeable);
        ccs.vehicleEnteringZone(vehicleOne);
        ccs.vehicleLeavingZone(vehicleOne);
        context.checking(new Expectations() {{
            exactly(1).of(chargeable).calculateChargeForTimeInZone(ccs.getVehicleRegistration().get(vehicleOne).getEventLog());
            will(returnValue(charge));
        }});
        ccs.calculateCharges();
    }

}
