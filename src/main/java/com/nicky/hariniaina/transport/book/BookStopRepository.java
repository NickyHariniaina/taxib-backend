package com.nicky.hariniaina.transport.book;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookStopRepository extends JpaRepository<BookStop, Long> {

  List<BookStop> findByDirectionIdOrderBySeqAsc(Long directionId);
}
