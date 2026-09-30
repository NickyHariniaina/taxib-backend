package com.nicky.hariniaina.transport.repo;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.nicky.hariniaina.transport.geo.GeoMath;
import com.nicky.hariniaina.transport.model.Stop;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class StopRepositoryIT {

  @Autowired private StopRepository stops;

  private Stop stop(String osmId, String name, double lat, double lon) {
    Stop s = new Stop();
    s.setOsmId(osmId);
    s.setName(name);
    s.setGeom(GeoMath.point(lat, lon));
    return stops.save(s);
  }

  @Test
  void nearbyOrdersByDistanceAndRespectsRadius() {
    // HEI area reference point.
    double lat = -18.8708;
    double lon = 47.5347;
    stop("osm-near", "Near", lat + 0.001, lon);
    stop("osm-far", "Far", lat + 0.01, lon);
    stop("osm-out", "Out", lat + 0.1, lon);

    List<Stop> within2km = stops.findNearby(lat, lon, 2_000, 5);
    assertEquals(List.of("Near", "Far"), within2km.stream().map(Stop::getName).toList());

    List<Stop> within500m = stops.findNearby(lat, lon, 500, 5);
    assertEquals(List.of("Near"), within500m.stream().map(Stop::getName).toList());
  }
}
