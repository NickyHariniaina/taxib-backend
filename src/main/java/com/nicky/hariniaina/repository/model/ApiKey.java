package com.nicky.hariniaina.repository.model;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.*;

@Entity
@Table(name = "api_keys")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiKey {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, unique = true)
  private String name;

  @Column(name = "key_hash", nullable = false, unique = true)
  private String keyHash;

  @Column(name = "requests_per_minute", nullable = false)
  private Integer requestsPerMinute;

  @Column(name = "requests_per_hour", nullable = false)
  private Integer requestsPerHour;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "revoked_at")
  private Instant revokedAt;
}
