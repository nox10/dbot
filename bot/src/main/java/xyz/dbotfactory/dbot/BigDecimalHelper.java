package xyz.dbotfactory.dbot;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class BigDecimalHelper {

    public static BigDecimal create(double number){
        return new BigDecimal(number).setScale(4, RoundingMode.HALF_UP);
    }

    public static BigDecimal create(int number){
        return new BigDecimal(number).setScale(4, RoundingMode.HALF_UP);
    }

    public static boolean isGreater(BigDecimal a, BigDecimal b){
        return a.compareTo(b) > 0;
    }

    public static boolean isSmaller(BigDecimal a, BigDecimal b){
        return a.compareTo(b) < 0;
    }

    public static boolean isGreaterOrEqual(BigDecimal a, BigDecimal b){
        return a.compareTo(b) >= 0;
    }

    public static boolean isSmallerOrEqual(BigDecimal a, BigDecimal b){
        return a.compareTo(b) <= 0;
    }

    public static boolean isGreater(BigDecimal a, double b){
        return isGreater(a, create(b));
    }

    public static boolean isSmaller(BigDecimal a, double b){
        return isSmaller(a, create(b));
    }

    public static boolean isGreaterOrEqual(BigDecimal a, double b){
        return isGreaterOrEqual(a, create(b));
    }


    public static boolean isSmallerOrEqual(BigDecimal a, double b){
        return isSmallerOrEqual(a, create(b));
    }

    public static boolean isGreater(BigDecimal a, int b){
        return isGreater(a, create(b));
    }

    public static boolean isSmaller(BigDecimal a, int b){
        return isSmaller(a, create(b));
    }

    public static boolean isGreaterOrEqual(BigDecimal a, int b){
        return isGreaterOrEqual(a, create(b));
    }


    public static boolean isSmallerOrEqual(BigDecimal a, int b){
        return isSmallerOrEqual(a, create(b));
    }

    public static boolean equals(BigDecimal shareAmount, double b) {
        return shareAmount.equals(create(b));
    }
}
