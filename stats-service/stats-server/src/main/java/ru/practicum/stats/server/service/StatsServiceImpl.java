package ru.practicum.stats.server.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.stats.dto.EndpointHitDto;
import ru.practicum.stats.dto.ViewStatsDto;
import ru.practicum.stats.server.model.EndpointHit;
import ru.practicum.stats.server.repository.StatsRepository;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StatsServiceImpl implements StatsService {

    private final StatsRepository statsRepository;

    @Override
    @Transactional
    public void add(EndpointHitDto endpointHitDto) {
        statsRepository.save(map(endpointHitDto));
    }

    @Override
    public List<ViewStatsDto> get(LocalDateTime start, LocalDateTime end, List<String> uris, boolean unique) {
        /* get db records */
        List<EndpointHit> data = uris == null || uris.isEmpty()
                ? statsRepository.get(start, end)
                : statsRepository.get(start, end, uris);

        /* key: obj(app, uri), value: list(ips) */
        Map<ViewStatsDto, List<String>> ipMap = new HashMap<>();
        data.forEach(eH -> {
            ViewStatsDto vSD = new ViewStatsDto(eH.getApp(), eH.getUri());
            if (!ipMap.containsKey(vSD)) {
                ipMap.put(vSD, new ArrayList<>());
            }
            ipMap.get(vSD).add(eH.getIp());
        });

        /* return ipMap's keys with 'hits' field updated from ipMap's values */
        return ipMap.entrySet()
                .stream()
                .map(unique ? entry -> {
                    ViewStatsDto key = entry.getKey();
                    key.setHits(new HashSet<>(entry.getValue()).size());
                    return key;
                } : entry -> {
                    ViewStatsDto key = entry.getKey();
                    key.setHits(entry.getValue().size());
                    return key;
                })
                .sorted((vSD1, vSD2) -> vSD2.getHits().compareTo(vSD1.getHits()))
                .collect(Collectors.toList());
    }

    @Override
    public Map<Integer, Integer> getViews(List<String> uris) {
        return statsRepository.countByUris(uris)
                .stream()
                .collect(Collectors.toMap(
                        list -> Integer.parseInt(list.get(0).substring(list.get(0).lastIndexOf("/") + 1)),
                        list -> Integer.parseInt(list.get(1))));
    }

    private EndpointHit map(EndpointHitDto endpointHitDto) {
        return new EndpointHit(null,
                endpointHitDto.getApp(),
                endpointHitDto.getUri(),
                endpointHitDto.getIp(),
                endpointHitDto.getTimestamp());
    }
}
