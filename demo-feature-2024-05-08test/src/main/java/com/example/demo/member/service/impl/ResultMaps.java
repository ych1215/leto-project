package com.example.demo.member.service.impl;

import java.util.List;
import java.util.Map;

public class ResultMaps {
    private Map<String, List<String>> periodsMap = null;
    private final Map<String, List<Double>> ratiosMap;

    public ResultMaps(Map<String, List<String>> periodsMap, Map<String, List<Double>> ratiosMap) {
        this.periodsMap = periodsMap;
        this.ratiosMap = ratiosMap;
    }

    public Map<String, List<String>> getPeriodsMap() {
        return periodsMap;
    }

    public Map<String, List<Double>> getRatiosMap() {
        return ratiosMap;
    }
}