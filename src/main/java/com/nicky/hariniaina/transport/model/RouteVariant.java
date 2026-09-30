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
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.locationtech.jts.geom.LineString;

@Entity
@Table(name = "route_variants")
@Getter
@Setter
@NoArgsConstructor
public class RouteVariant {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "line_id", nullable = false)
  private Line line;

  @Column(name = "osm_relation_id", nullable = false, unique = true)
  private Long osmRelationId;

  @Column(columnDefinition = "geography(LineString,4326)")
  private LineString geom;
}
