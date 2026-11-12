package com.nicky.hariniaina.endpoint.rest.controller;

import com.nicky.hariniaina.dto.NeighborhoodResponse;
import com.nicky.hariniaina.dto.NeighborhoodStopResponse;
import com.nicky.hariniaina.service.NeighborhoodService;
import com.nicky.hariniaina.service.StopService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/neighborhoods")
@RequiredArgsConstructor
public class NeighborhoodController {

  private final NeighborhoodService service;
  private final StopService stopService;

  @GetMapping
  public List<NeighborhoodResponse> getAllNeighborhoods() {
    return service.getAllNeighborhoods();
  }

  @GetMapping("/{name}/stops")
  public List<NeighborhoodStopResponse> getStopsByNeighborhood(@PathVariable String name) {
    return stopService.findByNeighborhood(name);
  }
}
