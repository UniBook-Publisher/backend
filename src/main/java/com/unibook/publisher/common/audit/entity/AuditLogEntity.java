package com.unibook.publisher.common.audit.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "audit_log")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class AuditLogEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "audit_id")
    private UUID id;

    @Column(name = "action", nullable = false)
    private String action;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "username", nullable = false)
    private String username;

    @Column(name = "method_name", nullable = false)
    private String methodName;

    @Column(name = "arguments", columnDefinition = "TEXT")
    private String arguments;

    @Column(name = "result", columnDefinition = "TEXT")
    private String result;

    @Column(name = "success", nullable = false)
    private boolean success;

    @Column(name = "exception_class")
    private String exceptionClass;

    @Column(name = "exception_message", columnDefinition = "TEXT")
    private String exceptionMessage;

    @Column(name = "timestamp", nullable = false)
    private Instant timestamp;

    @Column(name = "duration_ms", nullable = false)
    private long durationMs;
}
