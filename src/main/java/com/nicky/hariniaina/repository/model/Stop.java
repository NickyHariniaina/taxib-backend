package com.nicky.hariniaina.repository.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "stops")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Stop {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private String name;

  @Column(nullable = false)
  private Double latitude;

  @Column(nullable = false)
  private Double longitude;

  private String neighborhood;
}
