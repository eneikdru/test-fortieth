package com.eneik.generated.integration;

public class MessengerSubscriptionUpsert {
    private String userId;
    private String channelOrChatId;
    private String notificationType;
    private Boolean isActive;

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getChannelOrChatId() {
        return channelOrChatId;
    }

    public void setChannelOrChatId(String channelOrChatId) {
        this.channelOrChatId = channelOrChatId;
    }

    public String getNotificationType() {
        return notificationType;
    }

    public void setNotificationType(String notificationType) {
        this.notificationType = notificationType;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }
}
