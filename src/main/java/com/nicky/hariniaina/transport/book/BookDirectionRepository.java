package com.nicky.hariniaina.transport.book;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookDirectionRepository extends JpaRepository<BookDirection, Long> {

  List<BookDirection> findByLineId(Long lineId);

  void deleteByLineId(Long lineId);
}
