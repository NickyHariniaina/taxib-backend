package com.nicky.hariniaina.transport.book;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookLineRepository extends JpaRepository<BookLine, Long> {

  Optional<BookLine> findByRef(String ref);
}
