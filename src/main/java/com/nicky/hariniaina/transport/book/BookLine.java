package com.nicky.hariniaina.transport.book;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "book_lines")
@Getter
@Setter
@NoArgsConstructor
public class BookLine {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, unique = true)
  private String ref;

  private String operateur;

  private String tel;

  @Column(nullable = false)
  private String source = "ATT-2013";
}
