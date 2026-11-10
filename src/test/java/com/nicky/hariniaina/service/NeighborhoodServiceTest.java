
package com.nicky.hariniaina.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nicky.hariniaina.dto.NeighborhoodResponse;
import com.nicky.hariniaina.repository.NeighborhoodRepository;
import com.nicky.hariniaina.repository.model.Neighborhood;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NeighborhoodServiceTest {

  @Mock
  private NeighborhoodRepository repository;

  @InjectMocks
  private NeighborhoodService service;

  @Test
  void shouldReturnAllNeighborhoodsAsResponses() {
    var first = Neighborhood.builder()
        .id(1L)
        .name("Analakely")
        .latitude(-18.9087)
        .longitude(47.5256)
        .build();

    var second = Neighborhood.builder()
        .id(2L)
        .name("Ankadifotsy")
        .latitude(-18.9000)
        .longitude(47.5300)
        .build();

    when(repository.findAllByOrderByNameAsc()).thenReturn(List.of(first, second));

    List<NeighborhoodResponse> result = service.getAllNeighborhoods();

    assertThat(result)
        .containsExactly(
            new NeighborhoodResponse(1L, "Analakely", -18.9087, 47.5256),
            new NeighborhoodResponse(2L, "Ankadifotsy", -18.9000, 47.5300));

    verify(repository).findAllByOrderByNameAsc();
  }

  @Test
  void shouldReturnEmptyListWhenNoNeighborhoodExists() {
    when(repository.findAllByOrderByNameAsc()).thenReturn(List.of());

    List<NeighborhoodResponse> result = service.getAllNeighborhoods();

    assertThat(result).isEmpty();
    verify(repository).findAllByOrderByNameAsc();
  }
}
