package com.nicky.hariniaina.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.nicky.hariniaina.repository.model.Neighborhood;

@Repository
public interface NeighborhoodRepository extends JpaRepository<Neighborhood, Long>{

    List<Neighborhood> findAllByOrderByNameAsc();

}
