package com.ctags.config;

import com.ctags.model.Department;
import com.ctags.model.Role;
import com.ctags.model.User;
import com.ctags.repository.DepartmentRepository;
import com.ctags.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Adds demo departments and users the first time the app starts on an empty database. */
@Component
@ConditionalOnProperty(name = "ctags.seed", havingValue = "true")
public class DataSeeder implements CommandLineRunner {

    public static final String DEMO_PASSWORD = "Password@123";
    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final UserRepository users;
    private final DepartmentRepository departments;
    private final PasswordEncoder encoder;

    public DataSeeder(UserRepository users, DepartmentRepository departments, PasswordEncoder encoder) {
        this.users = users;
        this.departments = departments;
        this.encoder = encoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (users.count() > 0) {
            return;
        }
        Department engineering = departments.save(new Department("Engineering"));
        departments.save(new Department("Marketing"));

        String hash = encoder.encode(DEMO_PASSWORD);
        users.save(new User("System Admin", "admin@ctags.com", hash, Role.ADMIN));

        User manager = new User("Priya Manager", "manager@ctags.com", hash, Role.MANAGER);
        manager.setDepartment(engineering);
        manager = users.save(manager);

        for (String[] e : new String[][]{
                {"Rahul Employee", "employee@ctags.com"},
                {"Sneha Employee", "employee2@ctags.com"}}) {
            User emp = new User(e[0], e[1], hash, Role.EMPLOYEE);
            emp.setDepartment(engineering);
            emp.setManager(manager);
            users.save(emp);
        }
        log.info("Demo users created (password {}): admin@ctags.com, manager@ctags.com, "
                + "employee@ctags.com, employee2@ctags.com", DEMO_PASSWORD);
    }
}
