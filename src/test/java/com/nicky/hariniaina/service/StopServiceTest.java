package com.nicky.hariniaina.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nicky.hariniaina.dto.StopResponse;
import com.nicky.hariniaina.repository.StopRepository;
import com.nicky.hariniaina.repository.model.Stop;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StopServiceTest {

  @Mock private StopRepository repository;

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
    var far = stop(1L, "Pharmacie", -18.905, 47.53);
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
}
