package com.nicky.hariniaina.repository;

import com.nicky.hariniaina.PojaGenerated;
import com.nicky.hariniaina.repository.model.DummyUuid;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@PojaGenerated
@Repository
public interface DummyUuidRepository extends JpaRepository<DummyUuid, String> {
  @Override
  List<DummyUuid> findAllById(Iterable<String> ids);
}
