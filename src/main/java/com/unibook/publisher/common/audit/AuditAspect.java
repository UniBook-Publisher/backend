package com.unibook.publisher.common.audit;

import com.unibook.publisher.common.audit.entity.AuditLogEntity;
import com.unibook.publisher.common.audit.service.AuditLogService;
import com.unibook.publisher.common.logging.AppLogger;
import com.unibook.publisher.common.logging.LogMasker;
import com.unibook.publisher.common.security.AuthenticatedUser;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import java.io.InputStream;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Aspect
@Component
// Після ExposeInvocationInterceptor, але перед @PreAuthorize і @Transactional, щоб фіксувати і відмови в доступі
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class AuditAspect {
    private static final String ANONYMOUS = "anonymous";
    private static final int MAX_TEXT_LENGTH = 4000;
    private static final Pattern SENSITIVE_FIELD = Pattern.compile("(?i).*(password|secret|token).*");

    private final AuditLogService auditLogService;
    private final ObjectMapper objectMapper;
    private final AppLogger logger;

    public AuditAspect(AuditLogService auditLogService, ObjectMapper objectMapper, AppLogger logger) {
        this.auditLogService = auditLogService;
        this.objectMapper = objectMapper;
        this.logger = logger;
    }

    @Around("@annotation(auditAction)")
    public Object audit(ProceedingJoinPoint joinPoint, AuditAction auditAction) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Instant startedAt = Instant.now();

        AuditLogEntity entry = new AuditLogEntity();
        entry.setAction(auditAction.value().isBlank() ? signature.getName() : auditAction.value());
        entry.setMethodName(signature.getDeclaringType().getSimpleName() + "." + signature.getName());
        entry.setTimestamp(startedAt);
        fillCurrentUser(entry);
        entry.setArguments(toText(describeArguments(signature.getParameterNames(), joinPoint.getArgs())));

        try {
            Object result = joinPoint.proceed();
            entry.setSuccess(true);
            entry.setResult(toText(describe(result)));
            return result;
        } catch (Throwable ex) {
            entry.setSuccess(false);
            entry.setExceptionClass(ex.getClass().getName());
            entry.setExceptionMessage(truncate(ex.getMessage()));
            throw ex;
        } finally {
            entry.setDurationMs(Duration.between(startedAt, Instant.now()).toMillis());
            save(entry);
        }
    }

    private void save(AuditLogEntity entry) {
        try {
            auditLogService.save(entry);
        } catch (RuntimeException e) {
            logger.error("Не вдалося зберегти запис аудиту для {}", entry.getMethodName(), e);
        }
    }

    private void fillCurrentUser(AuditLogEntity entry) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication instanceof AnonymousAuthenticationToken) {
            entry.setUsername(ANONYMOUS);
            return;
        }
        if (authentication.getPrincipal() instanceof AuthenticatedUser user) {
            entry.setUserId(user.id());
            entry.setUsername(user.email());
            return;
        }
        entry.setUsername(authentication.getName());
    }

    private Map<String, Object> describeArguments(String[] names, Object[] args) {
        Map<String, Object> described = new LinkedHashMap<>();
        for (int i = 0; i < args.length; i++) {
            String name = names != null && i < names.length ? names[i] : "arg" + i;
            described.put(name, describe(args[i]));
        }
        return described;
    }

    private Object describe(Object value) {
        return switch (value) {
            case null -> null;
            case AuthenticatedUser user -> Map.of("id", user.id(), "email", user.email());
            case InputStream _, MultipartFile _, byte[] _ -> "<" + value.getClass().getSimpleName() + ">";
            default -> value;
        };
    }

    private String toText(Object value) {
        if (value == null) return null;
        try {
            JsonNode tree = objectMapper.valueToTree(value);
            maskSensitive(tree);
            return truncate(objectMapper.writeValueAsString(tree));
        } catch (RuntimeException e) {
            return "<" + value.getClass().getSimpleName() + ">";
        }
    }

    private void maskSensitive(JsonNode node) {
        if (node instanceof ObjectNode object) {
            for (String name : List.copyOf(object.propertyNames())) {
                if (SENSITIVE_FIELD.matcher(name).matches()) object.put(name, LogMasker.MASK);
                else maskSensitive(object.get(name));
            }
        } else if (node instanceof ArrayNode array) {
            array.values().forEach(this::maskSensitive);
        }
    }

    private String truncate(String text) {
        if (text == null || text.length() <= MAX_TEXT_LENGTH) return text;
        return text.substring(0, MAX_TEXT_LENGTH) + "...";
    }
}
