package com.nicky.hariniaina.service;

import com.nicky.hariniaina.dto.NeighborhoodStopResponse;
import com.nicky.hariniaina.dto.StopResponse;
import com.nicky.hariniaina.repository.NeighborhoodRepository;
import com.nicky.hariniaina.repository.StopRepository;
import com.nicky.hariniaina.repository.model.Stop;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class StopService {
  public static final int MAX_RADIUS_M = 5_000;
  public static final int MAX_LIMIT = 50;

  private static final double EARTH_RADIUS_M = 6_371_000;
  private static final double METERS_PER_DEGREE = 111_320;

  private final StopRepository repository;
  private final NeighborhoodRepository neighborhoods;

  public List<StopResponse> findNearby(double lat, double lon, int radiusM, int limit) {
    double radius = Math.min(Math.max(radiusM, 1), MAX_RADIUS_M);
    int max = Math.min(Math.max(limit, 1), MAX_LIMIT);
    double dLat = radius / METERS_PER_DEGREE;
    double dLng = radius / (METERS_PER_DEGREE * Math.max(Math.cos(Math.toRadians(lat)), 1e-6));

    return repository
        .findAllWithinBoundingBox(lat - dLat, lat + dLat, lon - dLng, lon + dLng)
        .stream()
        .map(s -> toResponse(s, lat, lon))
        .filter(r -> r.distanceM() <= radius)
        .sorted(Comparator.comparingLong(StopResponse::distanceM))
        .limit(max)
        .toList();
  }

  public List<NeighborhoodStopResponse> findByNeighborhood(String name) {
    if (neighborhoods.findByName(name).isEmpty()) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Unknown neighborhood: " + name);
    }
    return repository.findAllByNeighborhoodOrderByNameAsc(name).stream()
        .map(
            s ->
                new NeighborhoodStopResponse(
                    s.getId(), s.getName(), s.getLatitude(), s.getLongitude()))
        .toList();
  }

  private static StopResponse toResponse(Stop stop, double lat, double lon) {
    return new StopResponse(
        stop.getId(),
        stop.getName(),
        stop.getNeighborhood(),
        stop.getLatitude(),
        stop.getLongitude(),
        Math.round(distanceM(lat, lon, stop.getLatitude(), stop.getLongitude())));
  }

  static double distanceM(double lat1, double lon1, double lat2, double lon2) {
    double dLat = Math.toRadians(lat2 - lat1);
    double dLng = Math.toRadians(lon2 - lon1);
    double a =
        Math.sin(dLat / 2) * Math.sin(dLat / 2)
            + Math.cos(Math.toRadians(lat1))
                * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2)
                * Math.sin(dLng / 2);
    return 2 * EARTH_RADIUS_M * Math.asin(Math.sqrt(a));
  }
}
