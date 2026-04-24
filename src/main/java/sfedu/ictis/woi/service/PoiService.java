package sfedu.ictis.woi.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import sfedu.ictis.woi.infrastructure.client.GraphHopperClient;


@Slf4j
@Service
@RequiredArgsConstructor
public class PoiService {
    private final GraphHopperClient ghClient;

}
