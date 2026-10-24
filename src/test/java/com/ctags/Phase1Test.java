package com.ctags;

import com.ctags.model.AuditLog;
import com.ctags.model.Role;
import com.ctags.model.User;
import com.ctags.repository.AuditLogRepository;
import com.ctags.repository.TaskRepository;
import com.ctags.repository.UserRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Phase 1 tests: authentication, roles and basic task flow. Run with: mvn test */
@SpringBootTest(properties = {
        "ctags.seed=false",
        "spring.datasource.url=jdbc:h2:mem:ctags_test;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop"})
@AutoConfigureMockMvc
class Phase1Test {

    private static final String PW = "Password@123";

    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired TaskRepository tasks;
    @Autowired AuditLogRepository auditLogs;
    @Autowired PasswordEncoder encoder;

    Long employeeId;
    Long otherEmployeeId;

    @BeforeEach
    void setUp() {
        tasks.deleteAll();
        auditLogs.deleteAll();
        users.deleteAll(users.findAll().stream().filter(u -> u.getManager() != null).toList());
        users.deleteAll();

        String hash = encoder.encode(PW);
        users.save(new User("A", "admin@t.com", hash, Role.ADMIN));
        User manager = users.save(new User("M", "mgr@t.com", hash, Role.MANAGER));
        User emp = new User("E", "emp@t.com", hash, Role.EMPLOYEE);
        emp.setManager(manager);
        employeeId = users.save(emp).getId();
        otherEmployeeId = users.save(new User("X", "other@t.com", hash, Role.EMPLOYEE)).getId();
    }

    // ---------- helpers ----------

    private String token(String email) throws Exception {
        String body = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + PW + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return "Bearer " + JsonPath.read(body, "$.token");
    }

    private String future(int days) {
        return LocalDateTime.now().plusDays(days).truncatedTo(ChronoUnit.MINUTES).toString();
    }

    private ResultActions createTask(String auth, Long assignee, String deadline) throws Exception {
        String json = "{\"title\":\"Prepare Q3 report\",\"assigned_to\":" + assignee
                + ",\"priority\":\"high\",\"weightage\":3,\"deadline\":\"" + deadline + "\"}";
        return mvc.perform(post("/api/tasks").header("Authorization", auth)
                .contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private ResultActions setStatus(String auth, long taskId, String status) throws Exception {
        return mvc.perform(patch("/api/tasks/" + taskId + "/status").header("Authorization", auth)
                .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"" + status + "\"}"));
    }

    private long taskId(ResultActions result) throws Exception {
        Number id = JsonPath.read(result.andReturn().getResponse().getContentAsString(), "$.task.id");
        return id.longValue();
    }

    // ---------- authentication ----------

    @Test
    void loginWithWrongPasswordFails() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"emp@t.com\",\"password\":\"nope\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedRouteNeedsToken() throws Exception {
        mvc.perform(get("/api/tasks"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Missing token"));
    }

    @Test
    void onlyAdminRegistersUsers() throws Exception {
        String json = "{\"name\":\"New\",\"email\":\"new@t.com\",\"password\":\"longpassword\",\"role\":\"employee\"}";
        mvc.perform(post("/api/auth/register").header("Authorization", token("mgr@t.com"))
                        .contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/auth/register").header("Authorization", token("admin@t.com"))
                        .contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("employee"));
    }

    // ---------- tasks ----------

    @Test
    void managerAssignsAndEmployeeProgresses() throws Exception {
        String mgr = token("mgr@t.com");
        String emp = token("emp@t.com");
        long id = taskId(createTask(mgr, employeeId, future(3)).andExpect(status().isCreated()));

        mvc.perform(get("/api/tasks").header("Authorization", emp))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tasks.length()").value(1))
                .andExpect(jsonPath("$.tasks[0].assigned_by_name").value("M"));

        setStatus(emp, id, "in_progress").andExpect(status().isOk())
                .andExpect(jsonPath("$.task.status").value("in_progress"));
        setStatus(emp, id, "submitted").andExpect(status().isOk())
                .andExpect(jsonPath("$.task.status").value("submitted"));
    }

    @Test
    void employeeCannotAssign() throws Exception {
        createTask(token("emp@t.com"), employeeId, future(3)).andExpect(status().isForbidden());
    }

    @Test
    void managerLimitedToOwnTeam() throws Exception {
        createTask(token("mgr@t.com"), otherEmployeeId, future(3)).andExpect(status().isForbidden());
    }

    @Test
    void pastDeadlineRejected() throws Exception {
        createTask(token("mgr@t.com"), employeeId, future(-1)).andExpect(status().isBadRequest());
    }

    @Test
    void invalidStatusJumpRejected() throws Exception {
        long id = taskId(createTask(token("mgr@t.com"), employeeId, future(3)));
        setStatus(token("emp@t.com"), id, "graded").andExpect(status().isBadRequest());
    }

    @Test
    void auditLogRecordsAssignment() throws Exception {
        createTask(token("mgr@t.com"), employeeId, future(3)).andExpect(status().isCreated());
        assertThat(auditLogs.findAll()).extracting(AuditLog::getAction).contains("assign");
    }
}
