package com.unibook.publisher.common.audit;

import com.unibook.publisher.common.enums.UserRole;
import com.unibook.publisher.common.security.AuthenticatedUser;
import com.unibook.publisher.identity.entity.request.StaffRequest;
import com.unibook.publisher.identity.entity.response.UserResponse;
import com.unibook.publisher.identity.service.UserService;
import com.unibook.publisher.support.TestcontainersConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Map;
import java.util.UUID;

import static com.unibook.publisher.testutil.TestUsers.asUser;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Tag("audit")
@SpringBootTest(
        properties = {
                "spring.jpa.hibernate.ddl-auto=create-drop",
                "file-storage.type=LOCAL",
                "file-storage.location=build/test-uploads",
                "security.jwt.secret=audit-test-secret-key-that-is-long-enough-for-hs256-signatures",
                "security.jwt.expiration.access=1800000",
                "security.jwt.expiration.refresh=604800000",
                "s3.endpoint=http://localhost:9000",
                "s3.region=us-east-1",
                "s3.bucket=unibook-test",
                "s3.access-key=test-access-key",
                "s3.secret-key=test-secret-key"
        }
)
@Import(TestcontainersConfiguration.class)
class AuditIntegrationTest {
    private static final String PASSWORD = "StrongPass123";

    @Autowired
    private UserService userService;

    @Autowired
    private JdbcClient jdbc;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("createStaff: успішний виклик зберігає запис аудиту з результатом")
    void createStaff_Success_SavesAuditRecord() {
        AuthenticatedUser admin = authenticate(UserRole.ADMIN);
        String email = uniqueEmail();

        UserResponse response = userService.createStaff(staffRequest(email));

        Map<String, Object> record = auditRecord(email);
        assertThat(record.get("action")).isEqualTo("CREATE_STAFF");
        assertThat(record.get("method_name")).isEqualTo("UserServiceImpl.createStaff");
        assertThat(record.get("user_id")).isEqualTo(admin.id());
        assertThat(record.get("username")).isEqualTo(admin.email());
        assertThat(record.get("success")).isEqualTo(true);
        assertThat((String) record.get("arguments")).contains(email).doesNotContain(PASSWORD).contains("\"password\":\"****\"");
        assertThat((String) record.get("result")).contains(response.userId().toString());
        assertThat(record.get("exception_class")).isNull();
        assertThat(record.get("timestamp")).isNotNull();
    }

    @Test
    @DisplayName("createStaff: відмова в доступі зберігає запис аудиту і не приховує виняток")
    void createStaff_AccessDenied_SavesAuditRecordAndRethrows() {
        AuthenticatedUser author = authenticate(UserRole.AUTHOR);
        String email = uniqueEmail();

        assertThatThrownBy(() -> userService.createStaff(staffRequest(email)))
                .isInstanceOf(AccessDeniedException.class);

        Map<String, Object> record = auditRecord(email);
        assertThat(record.get("action")).isEqualTo("CREATE_STAFF");
        assertThat(record.get("user_id")).isEqualTo(author.id());
        assertThat(record.get("success")).isEqualTo(false);
        assertThat(record.get("result")).isNull();
        assertThat((String) record.get("exception_class")).contains("AuthorizationDeniedException");
        assertThat(record.get("exception_message")).isNotNull();
        assertThat(jdbc.sql("select count(*) from users where email = ?").param(email).query(Long.class).single())
                .isZero();
    }

    private AuthenticatedUser authenticate(UserRole role) {
        AuthenticatedUser user = asUser(UUID.randomUUID(), role);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
        return user;
    }

    private Map<String, Object> auditRecord(String email) {
        return jdbc.sql("select * from audit_log where arguments like ?")
                .param("%" + email + "%")
                .query()
                .singleRow();
    }

    private static String uniqueEmail() {
        return "audit-" + UUID.randomUUID() + "@unibook.test";
    }

    private static StaffRequest staffRequest(String email) {
        return new StaffRequest(email, PASSWORD, UserRole.EDITOR, "Audit Editor", null, null, null);
    }
}
