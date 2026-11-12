package com.nicky.hariniaina.service;

import com.nicky.hariniaina.dto.BusLineResponse;
import com.nicky.hariniaina.dto.StopResponse;
import com.nicky.hariniaina.repository.BusLineRepository;
import com.nicky.hariniaina.repository.model.BusLine;
import com.nicky.hariniaina.repository.model.Stop;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BusLineService {
  public static final int DEFAULT_RADIUS_M = 500;
  public static final int DEFAULT_LIMIT = 50;
  public static final int DEFAULT_STOP_LIMIT = 100;

  private static final int MAX_LIMIT = 200;
  private static final int MAX_STOP_LIMIT = 500;

  private final BusLineRepository repository;
  private final StopService stopService;

  /**
   * Lines of the catalogue. With a point, only the lines serving a stop within the radius, closest
   * line first.
   */
  @Transactional(readOnly = true)
  public List<BusLineResponse> list(
      String number, String query, Double lat, Double lon, int radiusM, int limit) {
    int max = Math.min(Math.max(limit, 1), MAX_LIMIT);

    if (lat != null && lon != null) {
      return nearby(lat, lon, radiusM, max);
    }

    List<BusLine> lines;
    if (number != null && !number.isBlank()) {
      lines = repository.findByNumber(number);
    } else if (query != null && !query.isBlank()) {
      lines = repository.search("%" + query.trim() + "%");
    } else {
      lines = repository.findAllByOrderByNumberAscNameAsc();
    }
    return lines.stream().limit(max).map(line -> toResponse(line, null)).toList();
  }

  /** Stops served by a bus number, closest first when a point is given. */
  @Transactional(readOnly = true)
  public List<StopResponse> stopsOf(String number, Double lat, Double lon, int limit) {
    int max = Math.min(Math.max(limit, 1), MAX_STOP_LIMIT);
    List<Stop> stops = repository.findStopsByNumber(number);

    if (lat == null || lon == null) {
      return stops.stream()
          .limit(max)
          .map(
              s ->
                  new StopResponse(
                      s.getId(),
                      s.getName(),
                      s.getNeighborhood(),
                      s.getLatitude(),
                      s.getLongitude(),
                      null))
          .toList();
    }
    return stops.stream()
        .map(
            s ->
                new StopResponse(
                    s.getId(),
                    s.getName(),
                    s.getNeighborhood(),
                    s.getLatitude(),
                    s.getLongitude(),
                    Math.round(StopService.distanceM(lat, lon, s.getLatitude(), s.getLongitude()))))
        .sorted(Comparator.comparingLong(StopResponse::distanceM))
        .limit(max)
        .toList();
  }

  private List<BusLineResponse> nearby(double lat, double lon, int radiusM, int limit) {
    Map<Long, Long> distances = new HashMap<>();
    for (Stop stop : stopService.findStopsWithinRadius(lat, lon, radiusM)) {
      distances.put(
          stop.getId(),
          Math.round(StopService.distanceM(lat, lon, stop.getLatitude(), stop.getLongitude())));
    }
    if (distances.isEmpty()) {
      return List.of();
    }

    return repository.findAllByStopIds(distances.keySet()).stream()
        .map(line -> toResponse(line, nearestDistance(line, distances)))
        .filter(line -> line.distanceM() != null)
        .sorted(Comparator.comparingLong(BusLineResponse::distanceM))
        .limit(limit)
        .toList();
  }

  private static Long nearestDistance(BusLine line, Map<Long, Long> distances) {
    return line.getStops().stream()
        .map(s -> distances.get(s.getId()))
        .filter(Objects::nonNull)
        .min(Long::compareTo)
        .orElse(null);
  }

  private static BusLineResponse toResponse(BusLine line, Long distanceM) {
    return new BusLineResponse(
        line.getNumber(),
        line.getName(),
        line.getDirection(),
        line.getOrigin(),
        line.getTerminus(),
        line.getColor(),
        line.getStopCount(),
        distanceM);
  }
}
