package com.nicky.hariniaina.repository.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;
import lombok.*;

@Entity
@Table(name = "bus_lines")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BusLine {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private String number;

  @Column(nullable = false)
  private String name;

  @Column(nullable = false)
  private String direction;

  @Column(nullable = false)
  private String origin;

  @Column(nullable = false)
  private String terminus;

  @Column(nullable = false)
  private String color;

  @Column(name = "stop_count", nullable = false)
  private Integer stopCount;

  @Column(name = "synced_at", nullable = false)
  private Instant syncedAt;

  @ManyToMany
  @JoinTable(
      name = "bus_line_stops",
      joinColumns = @JoinColumn(name = "line_id"),
      inverseJoinColumns = @JoinColumn(name = "stop_id"))
  @Builder.Default
  private Set<Stop> stops = new LinkedHashSet<>();
}
