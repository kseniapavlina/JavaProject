package com.trafficmon;

import java.math.BigDecimal;
import java.util.List;

public interface Chargeable {
    BigDecimal calculateChargeForTimeInZone(List<ZoneBoundaryCrossing> crossings);

}
