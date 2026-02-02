package com.inkling.model;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "chat_sessions")
public class ChatSession extends PanacheEntity {

    @Column(name = "session_id", unique = true, nullable = false)
    public String sessionId;

    @Column(name = "created_at")
    public LocalDateTime createdAt;

    @Column(name = "last_activity")
    public LocalDateTime lastActivity;

    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("createdAt ASC")
    public List<ChatMessage> messages = new ArrayList<>();

    public static ChatSession findBySessionId(String sessionId) {
        return find("sessionId", sessionId).firstResult();
    }

    public void addMessage(ChatMessage message) {
        messages.add(message);
        message.session = this;
        this.lastActivity = LocalDateTime.now();
    }
}
