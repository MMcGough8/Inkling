package com.inkling.model;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "chat_messages")
public class ChatMessage extends PanacheEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    public ChatSession session;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    public Role role;

    @Column(columnDefinition = "TEXT", nullable = false)
    public String content;

    @Column(name = "created_at")
    public LocalDateTime createdAt;

    public enum Role {
        USER,      // User's question
        ASSISTANT  // AI's response
    }

    public static ChatMessage userMessage(String content) {
        ChatMessage msg = new ChatMessage();
        msg.role = Role.USER;
        msg.content = content;
        msg.createdAt = LocalDateTime.now();
        return msg;
    }

    public static ChatMessage assistantMessage(String content) {
        ChatMessage msg = new ChatMessage();
        msg.role = Role.ASSISTANT;
        msg.content = content;
        msg.createdAt = LocalDateTime.now();
        return msg;
    }
}
