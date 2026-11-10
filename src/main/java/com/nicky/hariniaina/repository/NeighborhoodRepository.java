package com.nicky.hariniaina.repository;

import com.nicky.hariniaina.repository.model.Neighborhood;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NeighborhoodRepository extends JpaRepository<Neighborhood, Long> {

  List<Neighborhood> findAllByOrderByNameAsc();
}
