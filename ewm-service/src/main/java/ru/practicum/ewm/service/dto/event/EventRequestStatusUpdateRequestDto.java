package ru.practicum.ewm.service.dto.event;

import lombok.Data;

import java.util.List;

@Data
public class EventRequestStatusUpdateRequestDto {

    private List<Integer> requestIds;
    private String status;
}
