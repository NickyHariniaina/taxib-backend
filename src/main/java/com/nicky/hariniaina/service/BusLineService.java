package com.nicky.hariniaina.service;

import com.nicky.hariniaina.dto.BusLineResponse;
import com.nicky.hariniaina.dto.LineStopResponse;
import com.nicky.hariniaina.repository.BusLineRepository;
import com.nicky.hariniaina.repository.StopRepository;
import com.nicky.hariniaina.repository.model.BusLine;
import com.nicky.hariniaina.repository.model.Stop;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class BusLineService {
  public static final int DEFAULT_RADIUS_M = 500;
  public static final int DEFAULT_LIMIT = 50;
  public static final int DEFAULT_STOP_LIMIT = 100;

  private static final int MAX_LIMIT = 200;
  private static final int MAX_STOP_LIMIT = 500;

  private final BusLineRepository repository;
  private final StopRepository stopRepository;
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

  /** Lines serving one stop. */
  @Transactional(readOnly = true)
  public List<BusLineResponse> linesOfStop(long stopId) {
    if (!stopRepository.existsById(stopId)) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Unknown stop: " + stopId);
    }
    return repository.findAllByStopId(stopId).stream().map(line -> toResponse(line, null)).toList();
  }

  /** Stops served by a bus number, per variant and in route order. */
  @Transactional(readOnly = true)
  public List<LineStopResponse> stopsOf(String number, Double lat, Double lon, int limit) {
    int max = Math.min(Math.max(limit, 1), MAX_STOP_LIMIT);
    List<LineStopResponse> result = new ArrayList<>();

    for (BusLine variant : repository.findByNumberOrderByDirectionAscNameAsc(number)) {
      List<Stop> stops = variant.getStops();
      for (int position = 0; position < Math.min(max, stops.size()); position++) {
        Stop stop = stops.get(position);
        result.add(
            new LineStopResponse(
                stop.getId(),
                stop.getName(),
                stop.getNeighborhood(),
                stop.getLatitude(),
                stop.getLongitude(),
                variant.getDirection(),
                position,
                distanceOrNull(lat, lon, stop)));
      }
    }
    return result;
  }

  private static Long distanceOrNull(Double lat, Double lon, Stop stop) {
    if (lat == null || lon == null) {
      return null;
    }
    return Math.round(StopService.distanceM(lat, lon, stop.getLatitude(), stop.getLongitude()));
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

    // One query for the (line, stop) pairs: no lazy loading, no query per line.
    Map<Long, BusLine> lines = new LinkedHashMap<>();
    Map<Long, Long> nearest = new HashMap<>();
    for (Object[] pair : repository.findLineStopPairs(distances.keySet())) {
      var line = (BusLine) pair[0];
      var stop = (Stop) pair[1];
      var distance = distances.get(stop.getId());
      if (distance == null) {
        continue;
      }
      lines.putIfAbsent(line.getId(), line);
      nearest.merge(line.getId(), distance, Math::min);
    }

    return lines.values().stream()
        .map(line -> toResponse(line, nearest.get(line.getId())))
        .sorted(Comparator.comparingLong(BusLineResponse::distanceM))
        .limit(limit)
        .toList();
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
