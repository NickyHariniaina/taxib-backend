package com.nicky.hariniaina.transport.geo;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;

/** Pure geographic math. No Spring, no database — unit-testable. */
public final class GeoMath {

  public static final int SRID_WGS84 = 4326;
  private static final double EARTH_RADIUS_M = 6_371_000;

  private static final GeometryFactory FACTORY =
      new GeometryFactory(new PrecisionModel(), SRID_WGS84);

  private GeoMath() {}

  /** Great-circle distance in meters. Same formula as the web spike. */
  public static double haversineM(double lat1, double lon1, double lat2, double lon2) {
    double dLat = Math.toRadians(lat2 - lat1);
    double dLon = Math.toRadians(lon2 - lon1);
    double sin =
        Math.sin(dLat / 2) * Math.sin(dLat / 2)
            + Math.cos(Math.toRadians(lat1))
                * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2)
                * Math.sin(dLon / 2);
    return 2 * EARTH_RADIUS_M * Math.asin(Math.sqrt(sin));
  }

  /** JTS point with SRID set — required before persisting to geography columns. */
  public static Point point(double lat, double lon) {
    return FACTORY.createPoint(new Coordinate(lon, lat));
  }
}
