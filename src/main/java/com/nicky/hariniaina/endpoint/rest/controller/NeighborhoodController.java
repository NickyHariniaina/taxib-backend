package com.nicky.hariniaina.endpoint.rest.controller;

import com.nicky.hariniaina.dto.NeighborhoodResponse;
import com.nicky.hariniaina.service.NeighborhoodService;

import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/neighborhoods")
@RequiredArgsConstructor
public class NeighborhoodController {

    private final NeighborhoodService service;

    @GetMapping
    public List<NeighborhoodResponse> getAllNeighborhoods() {
        return service.getAllNeighborhoods();
    }
}
