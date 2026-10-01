package com.nicky.hariniaina.transport.book.model;

import java.util.List;

/** Full line detail. Returned by GET /book/lines/{ref}. */
public record BookLineDetailDto(
    String ref, String operateur, String tel, List<BookDirectionDto> directions) {}
