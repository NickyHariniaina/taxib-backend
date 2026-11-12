package com.nicky.hariniaina.repository;

import com.nicky.hariniaina.repository.model.Stop;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface StopRepository extends JpaRepository<Stop, Long> {

  @Query(
      """
      select s from Stop s
      where s.latitude between :minLat and :maxLat
        and s.longitude between :minLng and :maxLng
      """)
  List<Stop> findAllWithinBoundingBox(
      @Param("minLat") double minLat,
      @Param("maxLat") double maxLat,
      @Param("minLng") double minLng,
      @Param("maxLng") double maxLng);
}
