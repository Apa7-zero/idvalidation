package com.apa70.idvalidation.entity;

import java.util.Map;

public class Distance {
    private Map<Integer,Integer> positiveNumber;
    private Map<Integer,Integer> minusSign;

    public Map<Integer, Integer> getPositiveNumber() {
        return positiveNumber;
    }

    public void setPositiveNumber(Map<Integer, Integer> positiveNumber) {
        this.positiveNumber = positiveNumber;
    }

    public Map<Integer, Integer> getMinusSign() {
        return minusSign;
    }

    public void setMinusSign(Map<Integer, Integer> minusSign) {
        this.minusSign = minusSign;
    }
}
