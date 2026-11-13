package com.nicky.hariniaina.repository;

import com.nicky.hariniaina.repository.model.BusLine;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface BusLineRepository extends JpaRepository<BusLine, Long> {

  List<BusLine> findAllByOrderByNumberAscNameAsc();

  List<BusLine> findByNumber(String number);

  /**
   * Variants of one bus number, in a stable order: Aller before Retour.
   *
   * <p>Each variant's stops come back in route order (see {@code @OrderColumn} on
   * {@link BusLine#getStops()}), so callers can slice a ride between two stops.
   */
  List<BusLine> findByNumberOrderByDirectionAscNameAsc(String number);

  @Query(
      """
      select l from BusLine l
      where l.number ilike :pattern
         or l.name ilike :pattern
         or l.origin ilike :pattern
         or l.terminus ilike :pattern
      order by l.number, l.name
      """)
  List<BusLine> search(@Param("pattern") String pattern);

  @Query("select l, s from BusLine l join l.stops s where s.id in :stopIds")
  List<Object[]> findLineStopPairs(@Param("stopIds") Collection<Long> stopIds);

  @Query("select l from BusLine l join l.stops s where s.id = :stopId order by l.number, l.name")
  List<BusLine> findAllByStopId(@Param("stopId") Long stopId);
}
