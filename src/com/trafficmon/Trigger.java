package com.trafficmon;

import java.math.BigDecimal;

public class Trigger implements LibraryChecker {
    @Override
    public PenaltiesService libraryTrigger() {
         return OperationsTeam.getInstance();
    }

    @Override
    public void accountFor(Vehicle vehicle, BigDecimal charge) {
        try {
            RegisteredCustomerAccountsService.getInstance().accountFor(vehicle).deduct(charge);
        } catch (InsufficientCreditException | AccountNotRegisteredException ice) {
            OperationsTeam.getInstance().issuePenaltyNotice(vehicle, charge);
        }
    }
}
