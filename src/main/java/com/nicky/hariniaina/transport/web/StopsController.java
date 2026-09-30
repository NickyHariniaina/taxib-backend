package com.nicky.hariniaina.transport.web;

import com.nicky.hariniaina.transport.service.NearbyStopService;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/stops")
public class StopsController {

  private final NearbyStopService nearby;

  public StopsController(NearbyStopService nearby) {
    this.nearby = nearby;
  }

  /** Nearest stops first. Mirrors the web spike: top-N within a radius. */
  @GetMapping("/nearby")
  public List<StopDto> nearby(
      @RequestParam double lat,
      @RequestParam double lon,
      @RequestParam(defaultValue = "500") double radiusM,
      @RequestParam(defaultValue = "5") int limit) {
    if (lat < -90 || lat > 90 || lon < -180 || lon > 180) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "lat/lon out of range");
    }
    if (radiusM <= 0 || radiusM > 50_000) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "radiusM must be within (0, 50000]");
    }
    int safeLimit = Math.min(Math.max(limit, 1), 50);
    return nearby.nearby(lat, lon, radiusM, safeLimit);
  }
}
