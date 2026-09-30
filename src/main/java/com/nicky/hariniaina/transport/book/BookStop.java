package com.nicky.hariniaina.transport.book;

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
    name = "book_stops",
    uniqueConstraints = @UniqueConstraint(columnNames = {"direction_id", "seq"}))
@Getter
@Setter
@NoArgsConstructor
public class BookStop {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "direction_id", nullable = false)
  private BookDirection direction;

  @Column(nullable = false)
  private int seq;

  @Column(nullable = false)
  private String nom;
}
