package com.hiresense.api.db;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class MigrationIntegrityTest {

    private static final String JDBC_URL =
            System.getenv().getOrDefault("HIREDSENSE_TEST_DB_URL", "jdbc:postgresql://localhost:5433/hiresense");
    private static final String DB_USER = System.getenv().getOrDefault("HIREDSENSE_TEST_DB_USER", "hiresense");
    private static final String DB_PASSWORD = System.getenv().getOrDefault("HIREDSENSE_TEST_DB_PASSWORD", "hiresense");

    private static Connection connection;

    @BeforeAll
    static void resetSchemaAndOpenConnection() throws SQLException {
        connection = tryOpen();
        assumeTrue(
                connection != null,
                () -> "Postgres unreachable at " + JDBC_URL
                        + ". Start the compose stack: docker compose up -d postgres");

        Flyway.configure()
                .dataSource(JDBC_URL, DB_USER, DB_PASSWORD)
                .cleanDisabled(false)
                .load()
                .clean();
        Flyway.configure().dataSource(JDBC_URL, DB_USER, DB_PASSWORD).load().migrate();
    }

    @AfterAll
    static void closeConnection() throws SQLException {
        if (connection != null) {
            connection.close();
        }
    }

    private static Connection tryOpen() {
        try {
            return DriverManager.getConnection(JDBC_URL, DB_USER, DB_PASSWORD);
        } catch (SQLException e) {
            return null;
        }
    }

    private static int update(String sql, Object... args) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            bind(ps, args);
            return ps.executeUpdate();
        }
    }

    private static long insertReturningId(String sql, Object... args) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            bind(ps, args);
            ResultSet rs = ps.executeQuery();
            rs.next();
            return rs.getLong(1);
        }
    }

    private static void bind(PreparedStatement ps, Object... args) throws SQLException {
        for (int i = 0; i < args.length; i++) {
            ps.setObject(i + 1, args[i]);
        }
    }

    private static long createOrg(String slug) throws SQLException {
        return insertReturningId(
                "INSERT INTO organizations (name, slug) VALUES (?, ?) RETURNING id", "Org " + slug, slug);
    }

    private static long createUser(String email) throws SQLException {
        return insertReturningId(
                "INSERT INTO users (email, password_hash, full_name, platform_role)"
                        + " VALUES (?, ?, ?, ?) RETURNING id",
                email,
                "$2a$10$hashhashhashhashhashhashhashhashhashhashhashhashhash",
                "Test User",
                "CANDIDATE");
    }

    private static long createJob(long orgId, long userId, String title) throws SQLException {
        return insertReturningId(
                "INSERT INTO jobs (org_id, created_by, title, description)" + " VALUES (?, ?, ?, ?) RETURNING id",
                orgId,
                userId,
                title,
                "Description");
    }

    private static long createResume(long userId) throws SQLException {
        return insertReturningId(
                "INSERT INTO resumes (user_id, storage_key, original_filename, mime_type, file_size_bytes)"
                        + " VALUES (?, ?, ?, ?, ?) RETURNING id",
                userId,
                "resumes/" + userId + "/cv-" + System.nanoTime() + ".pdf",
                "cv.pdf",
                "application/pdf",
                1024L);
    }

    @Test
    void allMigrationsAppliedWithoutFailures() throws SQLException {
        try (PreparedStatement ps =
                connection.prepareStatement("SELECT COUNT(*) FROM flyway_schema_history WHERE success = FALSE")) {
            ResultSet rs = ps.executeQuery();
            rs.next();
            assertEquals(0, rs.getInt(1));
        }
    }

    @Test
    void refreshTokenHashColumnIsVariableLength() throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement("SELECT data_type FROM information_schema.columns"
                + " WHERE table_name = 'refresh_tokens' AND column_name = 'token_hash'")) {
            ResultSet rs = ps.executeQuery();
            rs.next();
            assertEquals("character varying", rs.getString(1));
        }
    }

    @Test
    void applicationIsUniquePerCandidateAndJob() throws SQLException {
        long org = createOrg("app-unique");
        long hrUser = createUser("hr-app-unique@example.com");
        long candidate = createUser("candidate-app-unique@example.com");
        long job = createJob(org, hrUser, "Backend Engineer");
        long resume = createResume(candidate);

        update("INSERT INTO applications (job_id, user_id, resume_id) VALUES (?, ?, ?)", job, candidate, resume);

        SQLException thrown = assertThrows(
                SQLException.class,
                () -> update(
                        "INSERT INTO applications (job_id, user_id, resume_id) VALUES (?, ?, ?)",
                        job,
                        candidate,
                        resume));
        assertEquals("23505", thrown.getSQLState());
    }

    @Test
    void orgMembershipIsUniquePerOrgAndUser() throws SQLException {
        long org = createOrg("member-unique");
        long user = createUser("member@example.com");

        update("INSERT INTO org_members (org_id, user_id, role) VALUES (?, ?, 'ORG_ADMIN')", org, user);

        SQLException thrown = assertThrows(
                SQLException.class,
                () -> update("INSERT INTO org_members (org_id, user_id, role) VALUES (?, ?, 'RECRUITER')", org, user));
        assertEquals("23505", thrown.getSQLState());
    }

    @Test
    void skillNamesAreCaseInsensitivelyUnique() throws SQLException {
        String uniqueBase = "ProbeSkill" + System.nanoTime();
        update("INSERT INTO skills (name, category) VALUES ('" + uniqueBase + "', 'Programming')");

        SQLException thrown = assertThrows(
                SQLException.class,
                () -> update("INSERT INTO skills (name, category) VALUES ('" + uniqueBase.toLowerCase()
                        + "', 'Programming')"));
        assertEquals("23505", thrown.getSQLState());
    }

    @Test
    void applicationMustReferenceExistingResume() throws SQLException {
        long org = createOrg("fk-resume");
        long hrUser = createUser("hr-fk@example.com");
        long candidate = createUser("candidate-fk@example.com");
        long job = createJob(org, hrUser, "Data Analyst");

        SQLException thrown = assertThrows(
                SQLException.class,
                () -> update(
                        "INSERT INTO applications (job_id, user_id, resume_id) VALUES (?, ?, ?)",
                        job,
                        candidate,
                        999999999L));
        assertEquals("23503", thrown.getSQLState());
    }

    @Test
    void invalidApplicationStatusIsRejectedByCheckConstraint() throws SQLException {
        long org = createOrg("status-check");
        long hrUser = createUser("hr-status@example.com");
        long candidate = createUser("candidate-status@example.com");
        long job = createJob(org, hrUser, "QA Engineer");
        long resume = createResume(candidate);

        SQLException thrown = assertThrows(
                SQLException.class,
                () -> update(
                        "INSERT INTO applications (job_id, user_id, resume_id, status)"
                                + " VALUES (?, ?, ?, 'MAGIC_STATUS')",
                        job,
                        candidate,
                        resume));
        assertEquals("23514", thrown.getSQLState());
    }

    @Test
    void jobsCannotRequireMaxExperienceBelowMin() throws SQLException {
        long org = createOrg("exp-check");
        long hrUser = createUser("hr-exp@example.com");

        SQLException thrown = assertThrows(
                SQLException.class,
                () -> update(
                        "INSERT INTO jobs (org_id, created_by, title, description, experience_min_months,"
                                + " experience_max_months) VALUES (?, ?, ?, ?, ?, ?)",
                        org,
                        hrUser,
                        "Senior Dev",
                        "Desc",
                        60,
                        24));
        assertEquals("23514", thrown.getSQLState());
    }
}
