package com.trafficmon;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

public class ChargeCalculator {

    private static final LocalTime TIME_BOUNDARY = LocalTime.of(14,0,0);
    private static final BigDecimal LOWER_FEE = new BigDecimal(4);
    private static final BigDecimal UPPER_FEE = new BigDecimal(6);
    private static final BigDecimal LONG_FEE = new BigDecimal(12);
    private static final int HOURS_IN_ZONE = 4;
    private BigDecimal charge = new BigDecimal(0);


    public BigDecimal getCharge(List<ZoneBoundaryCrossing> crossings) {
        calculateChargeForTimeInZone(crossings);
        return charge;
    }

    private void calculateChargeForTimeInZone(List<ZoneBoundaryCrossing> crossings) {
        ZoneBoundaryCrossing lastEvent = crossings.get(0);
        ArrayList<LocalTime> timesToCharge = new ArrayList<>();
        LocalTime criticalTime = crossings.get(1).timestamp();
        timesToCharge.add(lastEvent.timestamp());

        charge = timer(crossings) < HOURS_IN_ZONE ?
                calculateShortCharge(crossings, timesToCharge, criticalTime)
                : LONG_FEE;
    }

    private BigDecimal calculateShortCharge(List<ZoneBoundaryCrossing> crossings, ArrayList<LocalTime> timesToCharge, LocalTime criticalTime) {
        BigDecimal shortCharge = new BigDecimal(0);

        for (ZoneBoundaryCrossing crossing : crossings.subList(2, crossings.size())){
            if (crossing instanceof EntryEvent && calculateHoursBetween(criticalTime, crossing.timestamp()) > HOURS_IN_ZONE){
                int i = crossings.indexOf(crossing);
                criticalTime = crossings.get(i).timestamp();
                timesToCharge.add(crossing.timestamp());
            }
        }

        for (LocalTime time : timesToCharge){
            if (time.compareTo(TIME_BOUNDARY) <= 0) shortCharge = shortCharge.add(UPPER_FEE);
            else shortCharge = shortCharge.add(LOWER_FEE);
        }

        return shortCharge;
    }


    private double timer(List<ZoneBoundaryCrossing> crossings){
        double timer = 0;

        for (int i = 0; i < crossings.size()-1; i+=2){
            timer += calculateHoursBetween(crossings.get(i).timestamp(), crossings.get(i+1).timestamp());
        }

        return timer;
    }

    private double calculateHoursBetween(LocalTime startTimeMs, LocalTime endTimeMs) {
        return Math.ceil(ChronoUnit.MINUTES.between(startTimeMs, endTimeMs)) / 60.0;
    }

}