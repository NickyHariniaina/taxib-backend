package com.nicky.hariniaina.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nicky.hariniaina.repository.BusLineRepository;
import com.nicky.hariniaina.repository.StopRepository;
import com.nicky.hariniaina.repository.model.BusLine;
import com.nicky.hariniaina.repository.model.Stop;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class BusLineServiceTest {

  @Mock private BusLineRepository repository;

  @Mock private StopRepository stopRepository;

  @Mock private StopService stopService;

  @InjectMocks private BusLineService service;

  private static Stop stop(long id, String name, double lat, double lon) {
    return Stop.builder()
        .id(id)
        .name(name)
        .neighborhood("Analakely")
        .latitude(lat)
        .longitude(lon)
        .build();
  }

  private static BusLine line(long id, String number, Stop... stops) {
    return BusLine.builder()
        .id(id)
        .number(number)
        .name(number + " 67Ha")
        .direction("Aller")
        .origin("Ambohitromby")
        .terminus("Ny Havana")
        .color("#091f46")
        .stopCount(stops.length)
        .syncedAt(Instant.now())
        .stops(new LinkedHashSet<>(List.of(stops)))
        .build();
  }

  @Test
  void shouldListEveryLine() {
    when(repository.findAllByOrderByNumberAscNameAsc()).thenReturn(List.of(line(1L, "109")));

    var result = service.list(null, null, null, null, 500, 50);

    assertThat(result).extracting(l -> l.number()).containsExactly("109");
    assertThat(result.get(0).distanceM()).isNull();
  }

  @Test
  void shouldFilterByNumber() {
    when(repository.findByNumber("109")).thenReturn(List.of(line(1L, "109")));

    assertThat(service.list("109", null, null, null, 500, 50)).hasSize(1);
    verify(repository).findByNumber("109");
  }

  @Test
  void shouldSearchByQuery() {
    when(repository.search("%cit%")).thenReturn(List.of(line(1L, "109")));

    assertThat(service.list(null, "cit", null, null, 500, 50)).hasSize(1);
    verify(repository).search("%cit%");
  }

  @Test
  void shouldReturnLinesNearAPointWithDistance() {
    var close = stop(1L, "Ecole", -18.9001, 47.5001);
    var far = stop(2L, "Pharmacie", -18.895, 47.504);
    when(stopService.findStopsWithinRadius(-18.9, 47.5, 1_000)).thenReturn(List.of(close, far));
    when(repository.findAllByStopIds(any())).thenReturn(List.of(line(1L, "109", close, far)));

    var result = service.list(null, null, -18.9, 47.5, 1_000, 50);

    assertThat(result).hasSize(1);
    assertThat(result.get(0).distanceM()).isLessThan(100L);
  }

  @Test
  void shouldReturnNoLineWhenNoStopIsAround() {
    when(stopService.findStopsWithinRadius(-18.9, 47.5, 500)).thenReturn(List.of());

    assertThat(service.list(null, null, -18.9, 47.5, 500, 50)).isEmpty();
  }

  @Test
  void shouldReturnLinesOfStop() {
    when(stopRepository.existsById(1L)).thenReturn(true);
    when(repository.findAllByStopId(1L)).thenReturn(List.of(line(1L, "109")));

    var result = service.linesOfStop(1L);

    assertThat(result).extracting(l -> l.number()).containsExactly("109");
    assertThat(result.get(0).distanceM()).isNull();
  }

  @Test
  void shouldFailWhenStopIsUnknown() {
    when(stopRepository.existsById(42L)).thenReturn(false);

    assertThatThrownBy(() -> service.linesOfStop(42L))
        .isInstanceOf(ResponseStatusException.class)
        .hasFieldOrPropertyWithValue("statusCode", HttpStatus.NOT_FOUND);
  }

  @Test
  void shouldReturnStopsOfBusWithDistance() {
    var far = stop(1L, "Pharmacie", -18.895, 47.504);
    var close = stop(2L, "Ecole", -18.9001, 47.5001);
    when(repository.findStopsByNumber("109")).thenReturn(List.of(far, close));

    var result = service.stopsOf("109", -18.9, 47.5, 100);

    assertThat(result).extracting(s -> s.name()).containsExactly("Ecole", "Pharmacie");
    assertThat(result.get(0).distanceM()).isLessThan(result.get(1).distanceM());
  }

  @Test
  void shouldReturnStopsOfBusWithoutDistanceWhenNoPointIsGiven() {
    when(repository.findStopsByNumber("109")).thenReturn(List.of(stop(1L, "Ecole", -18.9, 47.5)));

    var result = service.stopsOf("109", null, null, 100);

    assertThat(result).hasSize(1);
    assertThat(result.get(0).distanceM()).isNull();
  }
}
