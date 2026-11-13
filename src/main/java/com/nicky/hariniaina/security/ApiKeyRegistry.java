package com.nicky.hariniaina.security;

import com.nicky.hariniaina.repository.ApiKeyRepository;
import com.nicky.hariniaina.repository.model.ApiKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Holds the active API keys in memory so a request costs one hash instead of one query.
 *
 * <p>The table is reloaded at most once per minute; the first load happens on the first request. An
 * empty table means "not enforcing yet": the API stays public until the first key is inserted,
 * which keeps the deployment safe during the rollout and makes revocation a single UPDATE.
 */
@Component
public class ApiKeyRegistry {

  static final Duration TTL = Duration.ofSeconds(60);

  private static final Logger log = LoggerFactory.getLogger(ApiKeyRegistry.class);

  private final ApiKeyRepository repository;
  private final Clock clock;
  private volatile Snapshot snapshot = new Snapshot(Map.of(), Instant.EPOCH);

  @Autowired
  public ApiKeyRegistry(ApiKeyRepository repository) {
    this(repository, Clock.systemUTC());
  }

  ApiKeyRegistry(ApiKeyRepository repository, Clock clock) {
    this.repository = repository;
    this.clock = clock;
  }

  /** Active keys by hash, refreshed every {@link #TTL}. Never blocks on a cold database twice. */
  public Map<String, ApiKey> activeKeys() {
    var current = snapshot;
    if (Duration.between(current.loadedAt(), clock.instant()).compareTo(TTL) < 0) {
      return current.keys();
    }
    return reload(current);
  }

  private synchronized Map<String, ApiKey> reload(Snapshot stale) {
    if (snapshot != stale) {
      return snapshot.keys();
    }
    try {
      var keys =
          repository.findAllByRevokedAtIsNull().stream()
              .collect(Collectors.toUnmodifiableMap(ApiKey::getKeyHash, key -> key));
      snapshot = new Snapshot(keys, clock.instant());
      if (keys.isEmpty()) {
        log.warn("No active API key stored: /api/** stays public until the first key is inserted");
      }
      return keys;
    } catch (RuntimeException e) {
      log.error("Cannot load the API keys, keeping the previous snapshot", e);
      snapshot = new Snapshot(stale.keys(), clock.instant());
      return stale.keys();
    }
  }

  public static String hash(String plainKey) {
    try {
      var digest =
          MessageDigest.getInstance("SHA-256").digest(plainKey.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(digest);
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 is always available", e);
    }
  }

  record Snapshot(Map<String, ApiKey> keys, Instant loadedAt) {}
}
