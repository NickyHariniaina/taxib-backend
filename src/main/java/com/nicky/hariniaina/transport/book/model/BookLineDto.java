package com.nicky.hariniaina.transport.book.model;

/** One bus line, without stops. Returned by GET /book/lines. */
public record BookLineDto(String ref, String operateur, String tel) {}
