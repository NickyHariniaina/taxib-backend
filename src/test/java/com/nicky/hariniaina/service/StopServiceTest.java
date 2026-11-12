package com.nicky.hariniaina.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nicky.hariniaina.dto.NeighborhoodStopResponse;
import com.nicky.hariniaina.dto.StopResponse;
import com.nicky.hariniaina.repository.NeighborhoodRepository;
import com.nicky.hariniaina.repository.StopRepository;
import com.nicky.hariniaina.repository.model.Neighborhood;
import com.nicky.hariniaina.repository.model.Stop;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class StopServiceTest {

  @Mock private StopRepository repository;

  @Mock private NeighborhoodRepository neighborhoods;

  @InjectMocks private StopService service;

  private static Stop stop(long id, String name, double lat, double lon) {
    return Stop.builder()
        .id(id)
        .name(name)
        .neighborhood("Analakely")
        .latitude(lat)
        .longitude(lon)
        .build();
  }

  @Test
  void shouldReturnClosestStopsFirst() {
    var far = stop(1L, "Pharmacie", -18.905, 47.504);
    var close = stop(2L, "Ecole", -18.9001, 47.5001);
    when(repository.findAllWithinBoundingBox(anyDouble(), anyDouble(), anyDouble(), anyDouble()))
        .thenReturn(List.of(far, close));

    var result = service.findNearby(-18.9, 47.5, 1_000, 10);

    assertThat(result).extracting(StopResponse::name).containsExactly("Ecole", "Pharmacie");
    assertThat(result.get(0).distanceM()).isLessThan(result.get(1).distanceM());
  }

  @Test
  void shouldExcludeStopsBeyondRadius() {
    when(repository.findAllWithinBoundingBox(anyDouble(), anyDouble(), anyDouble(), anyDouble()))
        .thenReturn(List.of(stop(1L, "Pharmacie", -18.891, 47.5)));

    assertThat(service.findNearby(-18.9, 47.5, 500, 10)).isEmpty();
  }

  @Test
  void shouldApplyLimit() {
    when(repository.findAllWithinBoundingBox(anyDouble(), anyDouble(), anyDouble(), anyDouble()))
        .thenReturn(List.of(stop(1L, "A", -18.9, 47.5), stop(2L, "B", -18.9, 47.5)));

    assertThat(service.findNearby(-18.9, 47.5, 500, 1)).hasSize(1);
    verify(repository).findAllWithinBoundingBox(anyDouble(), anyDouble(), anyDouble(), anyDouble());
  }

  @Test
  void shouldReturnStopsOfNeighborhood() {
    when(neighborhoods.findByName("Analakely"))
        .thenReturn(
            Optional.of(
                Neighborhood.builder()
                    .id(1L)
                    .name("Analakely")
                    .latitude(-18.9)
                    .longitude(47.5)
                    .build()));
    when(repository.findAllByNeighborhoodOrderByNameAsc("Analakely"))
        .thenReturn(List.of(stop(1L, "Boulangerie", -18.9, 47.5), stop(2L, "Ecole", -18.9, 47.5)));

    var result = service.findByNeighborhood("Analakely");

    assertThat(result)
        .extracting(NeighborhoodStopResponse::name)
        .containsExactly("Boulangerie", "Ecole");
  }

  @Test
  void shouldFailWhenNeighborhoodIsUnknown() {
    when(neighborhoods.findByName("Nowhere")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.findByNeighborhood("Nowhere"))
        .isInstanceOf(ResponseStatusException.class)
        .hasFieldOrPropertyWithValue("statusCode", HttpStatus.NOT_FOUND);
  }
}
