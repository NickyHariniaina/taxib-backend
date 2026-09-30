package com.nicky.hariniaina.transport.web;

/** Mirrors the web spike's stop shape — the frontend swaps one function body. */
public record StopDto(long id, String name, double lat, double lon, long distanceM) {}
