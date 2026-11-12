package com.nicky.hariniaina.dto;

public record StopResponse(
    Long id, String name, String neighborhood, Double lat, Double lon, Long distanceM) {}
