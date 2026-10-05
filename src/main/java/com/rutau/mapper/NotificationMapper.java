package com.rutau.mapper;

import com.rutau.dto.response.NotificationResponseDTO;
import com.rutau.model.Notification;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface NotificationMapper {

    NotificationResponseDTO toResponse(Notification notification);
}
