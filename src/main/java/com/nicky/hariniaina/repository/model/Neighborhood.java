package com.nicky.hariniaina.repository.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "neighborhoods")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Neighborhood {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, unique = true)
  private String name;

  @Column(nullable = false)
  private Double latitude;

  @Column(nullable = false)
  private Double longitude;
}
