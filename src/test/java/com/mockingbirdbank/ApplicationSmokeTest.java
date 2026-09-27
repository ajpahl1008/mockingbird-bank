package com.mockingbirdbank;

import com.mockingbirdbank.model.AccountHolder;
import com.mockingbirdbank.repository.AccountHolderRepository;
import com.mockingbirdbank.repository.AccountRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Full-context boot test against a real, throwaway Postgres - catches
 * wiring/config regressions (e.g. the env-var datasource placeholders) on
 * every run without depending on any developer's pre-existing local database.
 */
@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
class ApplicationSmokeTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AccountHolderRepository holderRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Test
    void healthEndpointReportsUp() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("\"status\":\"UP\"")));
    }

    @Test
    void dataInitializerSeedsOneHolderWithThreeAccounts() {
        List<AccountHolder> holders = holderRepository.findAll();

        assertThat(holders).hasSize(1);
        assertThat(accountRepository.findByHolderIdOrderByAccountTypeAscIdAsc(holders.get(0).getId()))
                .hasSize(3);
    }
}
