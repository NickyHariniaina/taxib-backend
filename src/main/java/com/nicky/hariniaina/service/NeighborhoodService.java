package com.nicky.hariniaina.service;

import com.nicky.hariniaina.dto.NeighborhoodResponse;
import com.nicky.hariniaina.repository.NeighborhoodRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NeighborhoodService {
  private final NeighborhoodRepository repository;

  public List<NeighborhoodResponse> getAllNeighborhoods() {
    return repository.findAllByOrderByNameAsc().stream()
        .map(
            neighborhood ->
                new NeighborhoodResponse(
                    neighborhood.getId(),
                    neighborhood.getName(),
                    neighborhood.getLatitude(),
                    neighborhood.getLongitude()))
        .toList();
  }
}
