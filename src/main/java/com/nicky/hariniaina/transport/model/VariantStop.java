package com.nicky.hariniaina.transport.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
    name = "variant_stops",
    uniqueConstraints = @UniqueConstraint(columnNames = {"variant_id", "seq"}))
@Getter
@Setter
@NoArgsConstructor
public class VariantStop {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "variant_id", nullable = false)
  private RouteVariant variant;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "stop_id", nullable = false)
  private Stop stop;

  @Column(nullable = false)
  private int seq;
}
