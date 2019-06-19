package com.agonyengine.forge.model;

import org.hibernate.annotations.GenericGenerator;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import java.util.Objects;
import java.util.UUID;

@Entity
public class Connection {
    @Id
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name = "uuid2", strategy = "uuid2")
    @Column(columnDefinition = "BINARY(16)")
    private UUID id;

    private String sessionUsername;
    private String sessionId;
    private String remoteAddress;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getSessionUsername() {
        return sessionUsername;
    }

    public void setSessionUsername(String sessionUsername) {
        this.sessionUsername = sessionUsername;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getRemoteAddress() {
        return remoteAddress;
    }

    public void setRemoteAddress(String remoteAddress) {
        this.remoteAddress = remoteAddress;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Connection)) return false;
        Connection that = (Connection) o;
        return Objects.equals(getId(), that.getId()) &&
            Objects.equals(getSessionUsername(), that.getSessionUsername()) &&
            Objects.equals(getSessionId(), that.getSessionId()) &&
            Objects.equals(getRemoteAddress(), that.getRemoteAddress());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId(), getSessionUsername(), getSessionId(), getRemoteAddress());
    }
}
