package com.nicky.hariniaina.endpoint.rest.controller;

import com.nicky.hariniaina.dto.BusLineResponse;
import com.nicky.hariniaina.dto.LineStopResponse;
import com.nicky.hariniaina.service.BusLineService;
import com.nicky.hariniaina.service.StopService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/lines")
@RequiredArgsConstructor
public class BusLineController {
  private final BusLineService service;

  @GetMapping
  public List<BusLineResponse> list(
      @RequestParam(required = false) String number,
      @RequestParam(required = false) String q,
      @RequestParam(required = false) @Min(-90) @Max(90) Double lat,
      @RequestParam(required = false) @Min(-180) @Max(180) Double lon,
      @RequestParam(defaultValue = "500") @Min(1) @Max(StopService.MAX_RADIUS_M) int radius,
      @RequestParam(defaultValue = "50") @Min(1) @Max(200) int limit) {
    return service.list(number, q, lat, lon, radius, limit);
  }

  @GetMapping("/{number}/stops")
  public List<LineStopResponse> stops(
      @PathVariable String number,
      @RequestParam(required = false) @Min(-90) @Max(90) Double lat,
      @RequestParam(required = false) @Min(-180) @Max(180) Double lon,
      @RequestParam(defaultValue = "100") @Min(1) @Max(500) int limit) {
    return service.stopsOf(number, lat, lon, limit);
  }
}
