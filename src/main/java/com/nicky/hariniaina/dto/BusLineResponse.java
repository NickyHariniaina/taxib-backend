package com.nicky.hariniaina.dto;

public record BusLineResponse(
    String number,
    String name,
    String direction,
    String origin,
    String terminus,
    String color,
    Integer stopCount,
    Long distanceM) {}
