package com.fulltack.zooManagment.Requests;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class EventRequest {

    @NotNull
    private String eventName;

    @NotNull
    private String eventDescription;

    @NotNull
    private LocalDateTime eventDate;

    @NotNull
    private String eventLocation;

    @NotNull
    private int capacity;

    @NotNull
    private String username;
}
