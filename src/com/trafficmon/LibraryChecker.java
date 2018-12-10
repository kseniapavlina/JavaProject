package com.trafficmon;

import java.math.BigDecimal;

public interface LibraryChecker {
    PenaltiesService libraryTrigger();
    void accountFor(Vehicle vehicle, BigDecimal charge);
}
