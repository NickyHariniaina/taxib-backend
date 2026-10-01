package com.nicky.hariniaina.transport.book.model;

import java.util.List;

/** One direction of a line with its ordered stops. */
public record BookDirectionDto(String sens, String nom, List<BookStopDto> stops) {}
