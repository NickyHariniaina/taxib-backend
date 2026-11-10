package com.nicky.hariniaina.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.nicky.hariniaina.dto.NeighborhoodResponse;
import com.nicky.hariniaina.repository.NeighborhoodRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class NeighborhoodService {
    private final NeighborhoodRepository repository;

    public List<NeighborhoodResponse> getAllNeighborhoods() {
        return repository.findAllByOrderByNameAsc().stream()
                .map(neighborhood -> new NeighborhoodResponse(neighborhood.getId(), neighborhood.getName(),
                        neighborhood.getLatitude(), neighborhood.getLongitude()))
                .toList();
    }
}
