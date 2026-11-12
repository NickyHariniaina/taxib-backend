package com.nicky.hariniaina.endpoint.rest.controller;

import com.nicky.hariniaina.dto.StopResponse;
import com.nicky.hariniaina.service.StopService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/stops")
@RequiredArgsConstructor
public class StopController {
  private final StopService service;

  @GetMapping
  public List<StopResponse> nearby(
      @RequestParam @Min(-90) @Max(90) double lat,
      @RequestParam @Min(-180) @Max(180) double lon,
      @RequestParam(defaultValue = "500") @Min(1) @Max(StopService.MAX_RADIUS_M) int radius,
      @RequestParam(defaultValue = "10") @Min(1) @Max(StopService.MAX_LIMIT) int limit) {
    return service.findNearby(lat, lon, radius, limit);
  }
}
