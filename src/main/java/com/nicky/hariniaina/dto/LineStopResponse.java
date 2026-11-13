package com.nicky.hariniaina.dto;

/**
 * A stop of a line variant, at its position along that variant.
 *
 * <p>{@code direction} and {@code order} are what tell an "Aller" stop #4 from a "Retour" stop #4:
 * the two directions are separate sequences, not one set.
 *
 * @param distanceM great-circle distance from the requested point, or null when no point was given
 */
public record LineStopResponse(
    Long id,
    String name,
    String neighborhood,
    Double lat,
    Double lon,
    String direction,
    Integer order,
    Long distanceM) {}
