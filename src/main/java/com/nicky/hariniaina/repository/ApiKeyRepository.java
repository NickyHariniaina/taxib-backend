package com.nicky.hariniaina.repository;

import com.nicky.hariniaina.repository.model.ApiKey;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApiKeyRepository extends JpaRepository<ApiKey, Long> {

  List<ApiKey> findAllByRevokedAtIsNull();
}
