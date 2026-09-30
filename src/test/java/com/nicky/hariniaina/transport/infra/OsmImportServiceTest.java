package com.nicky.hariniaina.transport.infra;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nicky.hariniaina.transport.infra.OsmImportService.ParsedStop;
import com.nicky.hariniaina.transport.model.Stop;
import com.nicky.hariniaina.transport.repo.StopRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OsmImportServiceTest {

  private static final String SAMPLE =
      """
      {"elements": [
        {"type": "node", "id": 1, "lat": -18.87, "lon": 47.53,
         "tags": {"highway": "bus_stop", "name": "Analakely"}},
        {"type": "node", "id": 2, "lat": -18.88, "lon": 47.54,
         "tags": {"highway": "bus_stop"}},
        {"type": "node", "id": 3, "tags": {"highway": "bus_stop"}},
        {"type": "way", "id": 9}
      ]}""";

  @Mock private StopRepository stops;

  @Test
  void parsesNamedUnnamedAndSkipsCoordless() throws Exception {
    var service = new OsmImportService(stops, new ObjectMapper());
    List<ParsedStop> parsed = service.parseStops(SAMPLE);
    assertEquals(2, parsed.size());
    assertEquals(new ParsedStop("node/1", "Analakely", -18.87, 47.53), parsed.get(0));
    assertEquals("Unnamed stop", parsed.get(1).name());
  }

  @Test
  void insertsNewAndRefreshesKnown() {
    var service = new OsmImportService(stops, new ObjectMapper());
    Stop known = new Stop();
    known.setOsmId("node/1");
    when(stops.findByOsmId("node/1")).thenReturn(Optional.of(known));
    when(stops.findByOsmId("node/2")).thenReturn(Optional.empty());

    int inserted =
        service.importStops(
            List.of(
                new ParsedStop("node/1", "Analakely R", -18.87, 47.53),
                new ParsedStop("node/2", "New", -18.88, 47.54)));

    assertEquals(1, inserted);
    assertEquals("Analakely R", known.getName());
    verify(stops).save(any(Stop.class));
    verify(stops, never()).save(known);
  }

  @Test
  void savedStopCarriesSrid() {
    var service = new OsmImportService(stops, new ObjectMapper());
    when(stops.findByOsmId("node/2")).thenReturn(Optional.empty());
    service.importStops(List.of(new ParsedStop("node/2", "New", -18.88, 47.54)));
    var captor = ArgumentCaptor.forClass(Stop.class);
    verify(stops).save(captor.capture());
    assertEquals(4326, captor.getValue().getGeom().getSRID());
  }
}
