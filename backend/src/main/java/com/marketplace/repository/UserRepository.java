package com.marketplace.repository;

import com.marketplace.jooq.tables.records.UsersRecord;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

import java.util.Optional;

import static com.marketplace.jooq.Tables.USERS;
import static org.jooq.impl.DSL.lower;

@Repository
public class UserRepository {
    private final DSLContext dsl;

    public UserRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    public Optional<UsersRecord> findByEmail(String email) {
        return dsl.selectFrom(USERS).where(lower(USERS.EMAIL).eq(email)).fetchOptional();
    }

    public Optional<UsersRecord> findForLogin(String identifier) {
        return identifier.contains("@") ? findByEmail(identifier)
                : dsl.selectFrom(USERS).where(lower(USERS.USERNAME).eq(identifier)).fetchOptional();
    }

    public UsersRecord create(String username, String email, String displayName, String passwordHash) {
        return dsl.insertInto(USERS).set(USERS.USERNAME, username).set(USERS.EMAIL, email).set(USERS.DISPLAY_NAME, displayName)
                .set(USERS.PASSWORD_HASH, passwordHash).returning().fetchSingle();
    }
}
