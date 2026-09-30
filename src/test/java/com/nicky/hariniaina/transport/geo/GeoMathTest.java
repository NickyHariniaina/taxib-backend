package com.nicky.hariniaina.transport.geo;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Point;

class GeoMathTest {

  @Test
  void oneDegreeOfLatitudeIsAbout111km() {
    assertEquals(111_195, GeoMath.haversineM(0, 0, 1, 0), 1.0);
  }

  @Test
  void samePointIsZero() {
    assertEquals(0, GeoMath.haversineM(-18.87, 47.53, -18.87, 47.53), 0.0001);
  }

  @Test
  void pointHasWgs84SridAndLonLatOrder() {
    Point p = GeoMath.point(-18.87, 47.53);
    assertEquals(4326, p.getSRID());
    assertEquals(-18.87, p.getY(), 1e-9);
    assertEquals(47.53, p.getX(), 1e-9);
  }
}
