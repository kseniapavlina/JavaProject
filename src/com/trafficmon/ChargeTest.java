package com.trafficmon;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

public class ChargeTest {

    private static final LocalTime TIME_BOUNDARY = LocalTime.of(14,0,0);
    private static final BigDecimal LOWER_FEE = new BigDecimal(4);
    private static final BigDecimal UPPER_FEE = new BigDecimal(6);
    private static final BigDecimal LONG_FEE = new BigDecimal(12);
    private static final int HOUR_BETWEEN = 4;
    private BigDecimal charge;

    public ChargeTest(){
        charge = new BigDecimal(0);
    }

    public BigDecimal getCharge(List<ZoneBoundaryCrossing> crossings) {
        calculateChargeForTimeInZone(crossings);
        return charge;
    }

    private void calculateChargeForTimeInZone(List<ZoneBoundaryCrossing> crossings){
        ZoneBoundaryCrossing lastEvent = crossings.get(0);
        ArrayList<LocalTime> timesToCharge = new ArrayList<>();
        LocalTime criticalTime = crossings.get(1).timestamp();
        timesToCharge.add(lastEvent.timestamp());

        if (timer(crossings) < HOUR_BETWEEN){
            chargeForShortTime(crossings, timesToCharge, criticalTime);
        }
        else charge = LONG_FEE;
    }

    private void chargeForShortTime(List<ZoneBoundaryCrossing> crossings, ArrayList<LocalTime> timesToCharge, LocalTime criticalTime) {
        for (ZoneBoundaryCrossing crossing : crossings.subList(2, crossings.size())){
            if (crossing instanceof EntryEvent && hoursBetween(criticalTime, crossing.timestamp()) > HOUR_BETWEEN){
                int i = crossings.indexOf(crossing);
                criticalTime = crossings.get(i).timestamp();
                timesToCharge.add(crossing.timestamp());
            }
        }
        for (LocalTime time : timesToCharge){
            if (compareTime(time, TIME_BOUNDARY) <= 0) charge = charge.add(UPPER_FEE);
            else charge = charge.add(LOWER_FEE);
        }
    }


    public double timer(List<ZoneBoundaryCrossing> crossings){
        double timer = 0;
        for (int i = 0; i < crossings.size()-1; i+=2){
            timer += hoursBetween(crossings.get(i).timestamp(), crossings.get(i+1).timestamp());
        }
        return timer;
    }

    //Quick Maffs
    private double hoursBetween(LocalTime startTimeMs, LocalTime endTimeMs) {
        return Math.ceil(ChronoUnit.MINUTES.between(startTimeMs, endTimeMs)) / 60.0;
    }

    private int compareTime(LocalTime x, LocalTime y){
        return x.compareTo(y);
    }

        public double getter(LocalTime startTimeMs, LocalTime endTimeMs){
        return hoursBetween(startTimeMs, endTimeMs);
    }
}