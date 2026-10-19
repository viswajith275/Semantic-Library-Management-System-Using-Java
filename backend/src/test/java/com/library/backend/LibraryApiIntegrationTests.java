package com.library.backend;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.library.backend.dto.AuthDto.*;
import com.library.backend.dto.BookDto.*;
import com.library.backend.dto.LoanDto.*;
import com.library.backend.dto.MemberDto.*;
import com.library.backend.entity.CopyStatus;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class LibraryApiIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static String adminToken;
    private static String memberCode;
    private static Long memberId;
    private static final String ISBN = "9780743273565";
    private static final String BARCODE_1 = "BC-GATSBY-01";
    private static final String BARCODE_2 = "BC-GATSBY-02";
    private static Long loanId;

    @Test
    @Order(1)
    void testRootAdminLoginAndTokenGeneration() throws Exception {
        AdminLoginRequest request = new AdminLoginRequest("admin@library.local", "admin123");

        MvcResult result = mockMvc.perform(post("/api/auth/admin/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.token").isNotEmpty())
            .andExpect(jsonPath("$.role").value("ADMIN"))
            .andReturn();

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        adminToken = json.get("token").asText();
        assertThat(adminToken).isNotBlank();
    }

    @Test
    @Order(2)
    void testCreateMemberAndCodeLogin() throws Exception {
        CreateMemberRequest request = new CreateMemberRequest("Alice Reader", "alice@example.com");

        MvcResult result = mockMvc.perform(post("/api/admin/members")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.customCode").isNotEmpty())
            .andExpect(jsonPath("$.name").value("Alice Reader"))
            .andReturn();

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        memberId = json.get("id").asLong();
        memberCode = json.get("customCode").asText();
        assertThat(memberCode).startsWith("MEM-");

        // Member login with custom code
        MemberLoginRequest loginReq = new MemberLoginRequest(memberCode);
        mockMvc.perform(post("/api/auth/member/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginReq)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.role").value("MEMBER"))
            .andExpect(jsonPath("$.customCode").value(memberCode));
    }

    @Test
    @Order(3)
    void testCreateBookWithEmbeddingAndInitialCopies() throws Exception {
        CreateBookRequest request = new CreateBookRequest(
            ISBN,
            "The Great Gatsby",
            "F. Scott Fitzgerald",
            "Navy Blue",
            "A tragic story of Jay Gatsby and his unrequited love for Daisy Buchanan in the Roaring Twenties.",
            "Jay Gatsby, Daisy Buchanan, Nick Carraway",
            "Aisle 3, Shelf B",
            List.of(BARCODE_1, BARCODE_2)
        );

        mockMvc.perform(post("/api/admin/books")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.isbn").value(ISBN))
            .andExpect(jsonPath("$.totalCopies").value(2))
            .andExpect(jsonPath("$.availableCopies").value(2))
            .andExpect(jsonPath("$.availabilityStatus").value("AVAILABLE"));
    }

    @Test
    @Order(4)
    void testSemanticSearchAndSingleBookLookup() throws Exception {
        // Search using member code
        mockMvc.perform(get("/api/library/search")
                .header("X-Auth-Code", memberCode)
                .param("q", "roaring twenties Gatsby Daisy tragic love")
                .param("topK", "5"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].isbn").value(ISBN))
            .andExpect(jsonPath("$[0].similarityScore").isNumber())
            .andExpect(jsonPath("$[0].availabilityStatus").value("AVAILABLE"));

        // Single book lookup
        mockMvc.perform(get("/api/library/books/" + ISBN)
                .header("X-Auth-Code", memberCode))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.isbn").value(ISBN))
            .andExpect(jsonPath("$.copies.length()").value(2))
            .andExpect(jsonPath("$.availabilityStatus").value("AVAILABLE"));
    }

    @Test
    @Order(5)
    void testMemberPinAndUnpin() throws Exception {
        // Pin book
        mockMvc.perform(post("/api/member/pins/" + ISBN)
                .header("X-Auth-Code", memberCode))
            .andExpect(status().isCreated());

        // Get pinned books
        mockMvc.perform(get("/api/member/pins")
                .header("X-Auth-Code", memberCode))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].isbn").value(ISBN))
            .andExpect(jsonPath("$[0].availabilityStatus").value("AVAILABLE"));

        // Unpin book
        mockMvc.perform(delete("/api/member/pins/" + ISBN)
                .header("X-Auth-Code", memberCode))
            .andExpect(status().isNoContent());

        // Get pinned books again (should be empty)
        mockMvc.perform(get("/api/member/pins")
                .header("X-Auth-Code", memberCode))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @Order(6)
    void testShelfAnalytics() throws Exception {
        mockMvc.perform(get("/api/admin/shelves")
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].shelfLocation").value("Aisle 3, Shelf B"))
            .andExpect(jsonPath("$[0].totalCopies").value(2))
            .andExpect(jsonPath("$[0].availableCopies").value(2));
    }

    @Test
    @Order(7)
    void testCheckoutAndCirculation() throws Exception {
        // Checkout copy 1
        CheckoutRequest checkoutReq = new CheckoutRequest(BARCODE_1, memberCode, 7);
        MvcResult checkoutRes = mockMvc.perform(post("/api/admin/loans/checkout")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(checkoutReq)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.barcode").value(BARCODE_1))
            .andReturn();

        JsonNode checkoutJson = objectMapper.readTree(checkoutRes.getResponse().getContentAsString());
        loanId = checkoutJson.get("loanId").asLong();

        // Check active loans for admin
        mockMvc.perform(get("/api/admin/loans/active")
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].barcode").value(BARCODE_1))
            .andExpect(jsonPath("$[0].memberCustomCode").value(memberCode));

        // Check active loans for member
        mockMvc.perform(get("/api/member/loans")
                .header("X-Auth-Code", memberCode))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].loanId").value(loanId))
            .andExpect(jsonPath("$[0].barcode").value(BARCODE_1));

        // Set copy 2 to MAINTENANCE to test ALL_BORROWED state machine
        UpdateCopyStatusRequest statusReq = new UpdateCopyStatusRequest(CopyStatus.MAINTENANCE);
        mockMvc.perform(patch("/api/admin/copies/" + BARCODE_2 + "/status")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(statusReq)))
            .andExpect(status().isOk());

        // Book should now have availableCopies == 0, borrowedCopies == 1, status == ALL_BORROWED
        mockMvc.perform(get("/api/library/books/" + ISBN)
                .header("X-Auth-Code", memberCode))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.availableCopies").value(0))
            .andExpect(jsonPath("$.borrowedCopies").value(1))
            .andExpect(jsonPath("$.availabilityStatus").value("ALL_BORROWED"))
            .andExpect(jsonPath("$.estimatedAvailableAt").isNotEmpty());
    }

    @Test
    @Order(8)
    void testExtendLoanAndReturnBook() throws Exception {
        // Extend loan
        ExtendLoanRequest extendReq = new ExtendLoanRequest(5);
        mockMvc.perform(patch("/api/admin/loans/" + loanId + "/extend")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(extendReq)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.message").value("Loan extended successfully"));

        // Return book
        ReturnRequest returnReq = new ReturnRequest(BARCODE_1);
        mockMvc.perform(post("/api/admin/loans/return")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(returnReq)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.message").value("Book returned successfully"));

        // Now copy 1 is available again
        mockMvc.perform(get("/api/library/books/" + ISBN)
                .header("X-Auth-Code", memberCode))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.availableCopies").value(1))
            .andExpect(jsonPath("$.availabilityStatus").value("AVAILABLE"));
    }

    @Test
    @Order(9)
    void testDeleteGuardsPreventForeignDataLoss() throws Exception {
        // Attempt to delete copy 1 (historical loan record exists) -> must fail with 409 Conflict
        mockMvc.perform(delete("/api/admin/copies/" + BARCODE_1)
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isConflict());

        // Attempt to delete book (historical loan record exists on copy 1) -> must fail with 409 Conflict
        mockMvc.perform(delete("/api/admin/books/" + ISBN)
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isConflict());
    }

    @Test
    @Order(10)
    void testAdminCreationAndLogout() throws Exception {
        // Create second admin
        CreateAdminRequest createAdminReq = new CreateAdminRequest("Assistant Librarian", "assistant@library.local", "securePass123");
        mockMvc.perform(post("/api/admin/users/create-admin")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createAdminReq)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.email").value("assistant@library.local"));

        // Admin logout
        mockMvc.perform(post("/api/auth/admin/logout")
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isNoContent());

        // Old token should now be unauthorized
        mockMvc.perform(get("/api/admin/shelves")
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isUnauthorized());
    }
}
