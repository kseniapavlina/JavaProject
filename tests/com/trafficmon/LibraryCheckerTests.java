package com.trafficmon;

import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.math.BigDecimal;

public class LibraryCheckerTests extends TestHelper{
    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Rule
    public JUnitRuleMockery context = new JUnitRuleMockery();
    private final LibraryChecker libraryChecker = context.mock(LibraryChecker.class);
    private PenaltiesService penaltiesService = context.mock(PenaltiesService.class);

    @Test
    public void testsLibraryTrigger() {
        CongestionChargeSystem ccs = new CongestionChargeSystem(libraryChecker);
        ccs.vehicleLeavingZone(vehicleOne);
        ccs.vehicleEnteringZone(vehicleOne);
        context.checking(new Expectations() {{
            exactly(1).of(libraryChecker).libraryTrigger();
            will(returnValue(penaltiesService));
            exactly(1).of(penaltiesService).triggerInvestigationInto(vehicleOne);

        }});
        ccs.calculateCharges();
    }

    @Test
    public void tests() {
        eventLogEntry(new EntryEvent(vehicleOne, getControllableClock(14, 30, 0)));
        eventLogEntry(new ExitEvent(vehicleOne, getControllableClock(18, 0, 0)));
        assert(new Trigger().libraryTrigger() instanceof PenaltiesService );
    }

    @Test
    public void testsUnregisteredVehicleException() throws AccountNotRegisteredException, InsufficientCreditException{
        thrown.expect(AccountNotRegisteredException.class);
        RegisteredCustomerAccountsService.getInstance().accountFor(vehicleTwo).deduct(BigDecimal.valueOf(1000000000));
    }

    @Test
    public void testsInsufficientCreditException() throws AccountNotRegisteredException, InsufficientCreditException {
        thrown.expect(InsufficientCreditException.class);
        RegisteredCustomerAccountsService.getInstance().accountFor(vehicleOne).deduct(BigDecimal.valueOf(1000000000));
    }
}
