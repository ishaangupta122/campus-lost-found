package com.campus.lostandfound.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.campus.lostandfound.model.Role;
import com.campus.lostandfound.model.User;
import com.campus.lostandfound.repository.UserRepository;
import com.jayway.jsonpath.JsonPath;

/**
 * End-to-end test: real Spring context, real security, real PostgreSQL.
 * It walks through the whole life of a found item. Because the test is
 * @Transactional, everything is rolled back afterwards and your data is untouched.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class FullFlowIntegrationTest {

    private static final String PASSWORD = "password123";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void foundItemIsClaimedApprovedAndReturned() throws Exception {
        String finderToken = registerAndLogin("Finder");
        String claimantToken = registerAndLogin("Claimant");
        String adminToken = createAdminAndLogin();

        // The finder reports a found item
        String itemJson = mockMvc.perform(post("/api/items")
                        .header("Authorization", "Bearer " + finderToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"itemName\":\"Keys\",\"description\":\"Two keys\",\"category\":\"Keys\","
                                + "\"type\":\"FOUND\",\"location\":\"Canteen\"}"))
                .andExpect(status().is2xxSuccessful())
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andReturn().getResponse().getContentAsString();
        int itemId = JsonPath.read(itemJson, "$.id");

        // Anyone can view it without logging in
        mockMvc.perform(get("/api/items/" + itemId)).andExpect(status().isOk());

        // The claimant claims it; the item becomes CLAIM_PENDING
        String claimJson = mockMvc.perform(post("/api/items/" + itemId + "/claims")
                        .header("Authorization", "Bearer " + claimantToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"One key has a red tag\"}"))
                .andExpect(status().is2xxSuccessful())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andReturn().getResponse().getContentAsString();
        int claimId = JsonPath.read(claimJson, "$.id");
        mockMvc.perform(get("/api/items/" + itemId))
                .andExpect(jsonPath("$.status").value("CLAIM_PENDING"));

        // The same person cannot claim twice
        mockMvc.perform(post("/api/items/" + itemId + "/claims")
                        .header("Authorization", "Bearer " + claimantToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"again\"}"))
                .andExpect(status().isConflict());

        // Students cannot use admin URLs; anonymous users cannot either
        mockMvc.perform(get("/api/admin/dashboard").header("Authorization", "Bearer " + claimantToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/admin/dashboard")).andExpect(status().isUnauthorized());

        // The admin approves, then confirms the handover
        mockMvc.perform(put("/api/admin/claims/" + claimId + "/decision")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"decision\":\"APPROVED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));
        mockMvc.perform(put("/api/admin/items/" + itemId + "/returned")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RETURNED"));

        mockMvc.perform(get("/api/admin/dashboard").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.returnedItems").isNumber());
    }

    @Test
    void protectedUrlsRejectMissingOrBadTokens() throws Exception {
        mockMvc.perform(get("/api/items/my")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/items/my").header("Authorization", "Bearer not-a-real-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void unknownItemGives404AndBadBodyGives400() throws Exception {
        String token = registerAndLogin("Someone");
        mockMvc.perform(get("/api/items/999999999")).andExpect(status().isNotFound());
        mockMvc.perform(post("/api/items")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"itemName\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors").exists());
    }

    @Test
    void registeringTwiceWithTheSameEmailGivesConflict() throws Exception {
        String email = "dup-" + UUID.randomUUID() + "@example.com";
        String body = "{\"name\":\"Dup\",\"email\":\"" + email + "\",\"password\":\"" + PASSWORD + "\"}";
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().is2xxSuccessful());
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict());
    }

    @Test
    void wrongPasswordIsUnauthorized() throws Exception {
        String email = "wrong-" + UUID.randomUUID() + "@example.com";
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"W\",\"email\":\"" + email + "\",\"password\":\"" + PASSWORD + "\"}"))
                .andExpect(status().is2xxSuccessful());
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"password\":\"wrong-password\"}"))
                .andExpect(status().isUnauthorized());
    }

    // ---- helpers ----

    private String registerAndLogin(String name) throws Exception {
        String email = name.toLowerCase() + "-" + UUID.randomUUID() + "@example.com";
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"" + name + "\",\"email\":\"" + email + "\",\"password\":\"" + PASSWORD + "\"}"))
                .andExpect(status().is2xxSuccessful());
        return login(email);
    }

    private String createAdminAndLogin() throws Exception {
        String email = "admin-" + UUID.randomUUID() + "@example.com";
        userRepository.save(new User("Test Admin", email, passwordEncoder.encode(PASSWORD), Role.ADMIN));
        return login(email);
    }

    private String login(String email) throws Exception {
        String json = mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(json, "$.token");
    }
}
