package com.nicky.hariniaina.transport.repo;

import com.nicky.hariniaina.transport.model.Stop;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StopRepository extends JpaRepository<Stop, Long> {

  Optional<Stop> findByOsmId(String osmId);

  /**
   * Stops within {@code radiusM} meters, nearest first (PostGIS KNN on GiST index). Point is built
   * lon-first: ST_MakePoint(:lon, :lat). Uses CAST(.. AS geography) rather than the ::geography
   * shorthand — Hibernate would parse the second colon as a named parameter.
   */
  @Query(
      value =
          "SELECT s.* FROM stops s WHERE ST_DWithin(s.geom, CAST(ST_SetSRID(ST_MakePoint(:lon,"
              + " :lat), 4326) AS geography), :radius) ORDER BY s.geom <->"
              + " CAST(ST_SetSRID(ST_MakePoint(:lon, :lat), 4326) AS geography) LIMIT :limit",
      nativeQuery = true)
  List<Stop> findNearby(
      @Param("lat") double lat,
      @Param("lon") double lon,
      @Param("radius") double radiusM,
      @Param("limit") int limit);
}
