package com.nicky.hariniaina.repository;

import com.nicky.hariniaina.repository.model.BusLine;
import com.nicky.hariniaina.repository.model.Stop;
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

  @Query("select distinct s from BusLine l join l.stops s where l.number = :number order by s.name")
  List<Stop> findStopsByNumber(@Param("number") String number);

  @Query("select l from BusLine l join l.stops s where s.id = :stopId order by l.number, l.name")
  List<BusLine> findAllByStopId(@Param("stopId") Long stopId);
}
