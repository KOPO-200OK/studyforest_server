package com.gongsoop.studyspace.dto.response;

import com.gongsoop.studyspace.entity.StudyChannel;

public record StudyChannelResponse(
        Long studyChannelId,
        Integer channelNo,
        String channelName
) {
    public static StudyChannelResponse from(StudyChannel channel) {
        return new StudyChannelResponse(channel.getId(), channel.getChannelNo(), channel.getName());
    }
}
