package com.nicky.hariniaina.transport.service;

import com.nicky.hariniaina.transport.geo.GeoMath;
import com.nicky.hariniaina.transport.repo.StopRepository;
import com.nicky.hariniaina.transport.web.StopDto;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NearbyStopService {

  private final StopRepository stops;

  public NearbyStopService(StopRepository stops) {
    this.stops = stops;
  }

  @Transactional(readOnly = true)
  public List<StopDto> nearby(double lat, double lon, double radiusM, int limit) {
    return stops.findNearby(lat, lon, radiusM, limit).stream()
        .map(
            s ->
                new StopDto(
                    s.getId(),
                    s.getName(),
                    s.getGeom().getY(),
                    s.getGeom().getX(),
                    Math.round(
                        GeoMath.haversineM(lat, lon, s.getGeom().getY(), s.getGeom().getX()))))
        .toList();
  }
}
