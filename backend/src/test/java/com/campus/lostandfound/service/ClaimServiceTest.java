package com.campus.lostandfound.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.campus.lostandfound.dto.ClaimRequest;
import com.campus.lostandfound.dto.ClaimResponse;
import com.campus.lostandfound.dto.ItemResponse;
import com.campus.lostandfound.exception.BadRequestException;
import com.campus.lostandfound.exception.ConflictException;
import com.campus.lostandfound.exception.ForbiddenException;
import com.campus.lostandfound.model.Claim;
import com.campus.lostandfound.model.ClaimStatus;
import com.campus.lostandfound.model.Item;
import com.campus.lostandfound.model.ItemStatus;
import com.campus.lostandfound.model.ItemType;
import com.campus.lostandfound.model.Role;
import com.campus.lostandfound.model.User;
import com.campus.lostandfound.repository.ClaimRepository;
import com.campus.lostandfound.repository.ItemRepository;
import com.campus.lostandfound.repository.UserRepository;

/** Unit tests of the claim rules with fake repositories (no database). */
class ClaimServiceTest {

    private ClaimRepository claimRepository;
    private ItemRepository itemRepository;
    private UserRepository userRepository;
    private ClaimService claimService;

    private User finder;     // id 1: reported the found item
    private User claimant;   // id 2
    private User otherStudent; // id 4
    private User admin;      // id 3
    private Item item;       // id 10, FOUND, OPEN

    @BeforeEach
    void setUp() {
        claimRepository = mock(ClaimRepository.class);
        itemRepository = mock(ItemRepository.class);
        userRepository = mock(UserRepository.class);
        claimService = new ClaimService(claimRepository, itemRepository, userRepository);

        finder = fakeUser(1L, "Finder", Role.STUDENT);
        claimant = fakeUser(2L, "Claimant", Role.STUDENT);
        admin = fakeUser(3L, "Admin", Role.ADMIN);
        otherStudent = fakeUser(4L, "Other", Role.STUDENT);

        item = new Item("Keys", "Two keys", "Keys", ItemType.FOUND, "Canteen", LocalDate.now(), finder);
        ReflectionTestUtils.setField(item, "id", 10L);
        when(itemRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(item));
        when(itemRepository.save(any(Item.class))).thenAnswer(call -> call.getArgument(0));
        when(claimRepository.save(any(Claim.class))).thenAnswer(call -> call.getArgument(0));
    }

    private User fakeUser(Long id, String name, Role role) {
        User user = mock(User.class);
        when(user.getId()).thenReturn(id);
        when(user.getName()).thenReturn(name);
        when(user.getRole()).thenReturn(role);
        when(userRepository.findById(id)).thenReturn(Optional.of(user));
        return user;
    }

    private Claim claimWithId(long claimId, User owner) {
        Claim claim = new Claim(item, owner, "It is mine");
        ReflectionTestUtils.setField(claim, "id", claimId);
        when(claimRepository.findById(claimId)).thenReturn(Optional.of(claim));
        return claim;
    }

    // ------------------------------------------------------------ submitting

    @Test
    void studentCanClaimFoundItem() {
        ClaimResponse response = claimService.submitClaim(2L, 10L, new ClaimRequest("  It is mine  "));

        assertEquals(ClaimStatus.PENDING, response.status());
        assertEquals("It is mine", response.message());
        assertEquals(ItemStatus.CLAIM_PENDING, item.getStatus());
        verify(itemRepository).save(item);
    }

    @Test
    void adminCannotSubmitClaim() {
        assertThrows(ForbiddenException.class,
                () -> claimService.submitClaim(3L, 10L, new ClaimRequest("mine")));
    }

    @Test
    void lostItemsCannotBeClaimed() {
        Item lost = new Item("Phone", "Black phone", "Phones", ItemType.LOST, "Library", LocalDate.now(), finder);
        ReflectionTestUtils.setField(lost, "id", 11L);
        when(itemRepository.findByIdForUpdate(11L)).thenReturn(Optional.of(lost));

        assertThrows(BadRequestException.class,
                () -> claimService.submitClaim(2L, 11L, new ClaimRequest("mine")));
    }

    @Test
    void cannotClaimOwnReport() {
        assertThrows(ForbiddenException.class,
                () -> claimService.submitClaim(1L, 10L, new ClaimRequest("mine")));
    }

    @Test
    void returnedOrClosedItemsCannotBeClaimed() {
        item.setStatus(ItemStatus.RETURNED);
        assertThrows(ConflictException.class,
                () -> claimService.submitClaim(2L, 10L, new ClaimRequest("mine")));

        item.setStatus(ItemStatus.CLOSED);
        assertThrows(ConflictException.class,
                () -> claimService.submitClaim(2L, 10L, new ClaimRequest("mine")));
    }

    @Test
    void duplicatePendingClaimIsRejected() {
        when(claimRepository.existsByItemIdAndClaimantIdAndStatus(10L, 2L, ClaimStatus.PENDING)).thenReturn(true);

        assertThrows(ConflictException.class,
                () -> claimService.submitClaim(2L, 10L, new ClaimRequest("again")));
        verify(claimRepository, never()).save(any(Claim.class));
    }

    @Test
    void cannotClaimItemThatAlreadyHasApprovedClaim() {
        when(claimRepository.existsByItemIdAndStatus(10L, ClaimStatus.APPROVED)).thenReturn(true);

        assertThrows(ConflictException.class,
                () -> claimService.submitClaim(2L, 10L, new ClaimRequest("mine")));
    }

    @Test
    void secondStudentCanAlsoClaimWhileFirstIsPending() {
        item.setStatus(ItemStatus.CLAIM_PENDING);

        ClaimResponse response = claimService.submitClaim(4L, 10L, new ClaimRequest("it is mine"));

        assertEquals(ClaimStatus.PENDING, response.status());
    }

    // ------------------------------------------------------------ viewing

    @Test
    void claimIsVisibleToClaimantFinderAndAdminOnly() {
        claimWithId(100L, claimant);

        assertEquals(100L, claimService.getClaim(2L, 100L).id());
        assertEquals(100L, claimService.getClaim(1L, 100L).id());
        assertEquals(100L, claimService.getClaim(3L, 100L).id());
        assertThrows(ForbiddenException.class, () -> claimService.getClaim(4L, 100L));
    }

    @Test
    void onlyAdminsCanListPendingClaims() {
        Claim pending = claimWithId(100L, claimant);
        when(claimRepository.findByStatusOrderByCreatedAtAsc(ClaimStatus.PENDING)).thenReturn(List.of(pending));

        assertEquals(1, claimService.listPendingClaims(3L).size());
        assertThrows(ForbiddenException.class, () -> claimService.listPendingClaims(2L));
    }

    // ------------------------------------------------------------ deciding

    @Test
    void approvingRejectsOtherPendingClaimsAndKeepsItemPending() {
        Claim winner = claimWithId(100L, claimant);
        Claim loser = claimWithId(101L, otherStudent);
        item.setStatus(ItemStatus.CLAIM_PENDING);
        when(claimRepository.findByItemIdAndStatus(10L, ClaimStatus.PENDING)).thenReturn(List.of(winner, loser));

        ClaimResponse response = claimService.decideClaim(3L, 100L, ClaimStatus.APPROVED);

        assertEquals(ClaimStatus.APPROVED, response.status());
        assertEquals(ClaimStatus.APPROVED, winner.getStatus());
        assertEquals(ClaimStatus.REJECTED, loser.getStatus());
        assertEquals(ItemStatus.CLAIM_PENDING, item.getStatus());
    }

    @Test
    void secondApprovalForSameItemIsRefused() {
        claimWithId(100L, claimant);
        when(claimRepository.existsByItemIdAndStatus(10L, ClaimStatus.APPROVED)).thenReturn(true);

        assertThrows(ConflictException.class, () -> claimService.decideClaim(3L, 100L, ClaimStatus.APPROVED));
    }

    @Test
    void rejectingLastWaitingClaimReopensItem() {
        Claim claim = claimWithId(100L, claimant);
        item.setStatus(ItemStatus.CLAIM_PENDING);

        claimService.decideClaim(3L, 100L, ClaimStatus.REJECTED);

        assertEquals(ClaimStatus.REJECTED, claim.getStatus());
        assertEquals(ItemStatus.OPEN, item.getStatus());
    }

    @Test
    void rejectingWhileOthersWaitKeepsItemPending() {
        claimWithId(100L, claimant);
        item.setStatus(ItemStatus.CLAIM_PENDING);
        when(claimRepository.existsByItemIdAndStatus(10L, ClaimStatus.PENDING)).thenReturn(true);

        claimService.decideClaim(3L, 100L, ClaimStatus.REJECTED);

        assertEquals(ItemStatus.CLAIM_PENDING, item.getStatus());
    }

    @Test
    void alreadyDecidedClaimCannotBeDecidedAgain() {
        Claim claim = claimWithId(100L, claimant);
        claim.setStatus(ClaimStatus.REJECTED);

        assertThrows(ConflictException.class, () -> claimService.decideClaim(3L, 100L, ClaimStatus.APPROVED));
    }

    @Test
    void claimOfClosedItemCannotBeDecided() {
        claimWithId(100L, claimant);
        item.setStatus(ItemStatus.CLOSED);

        assertThrows(ConflictException.class, () -> claimService.decideClaim(3L, 100L, ClaimStatus.APPROVED));
    }

    @Test
    void decisionMustBeApprovedOrRejected() {
        claimWithId(100L, claimant);

        assertThrows(BadRequestException.class, () -> claimService.decideClaim(3L, 100L, ClaimStatus.PENDING));
    }

    @Test
    void studentsCannotDecideClaims() {
        claimWithId(100L, claimant);

        assertThrows(ForbiddenException.class, () -> claimService.decideClaim(1L, 100L, ClaimStatus.APPROVED));
    }

    // ------------------------------------------------------------ returning

    @Test
    void finderCanMarkItemReturnedAfterApproval() {
        item.setStatus(ItemStatus.CLAIM_PENDING);
        when(claimRepository.existsByItemIdAndStatus(10L, ClaimStatus.APPROVED)).thenReturn(true);

        ItemResponse response = claimService.markItemReturned(1L, 10L);

        assertEquals(ItemStatus.RETURNED, response.status());
    }

    @Test
    void adminCanAlsoMarkItemReturned() {
        item.setStatus(ItemStatus.CLAIM_PENDING);
        when(claimRepository.existsByItemIdAndStatus(10L, ClaimStatus.APPROVED)).thenReturn(true);

        assertEquals(ItemStatus.RETURNED, claimService.markItemReturned(3L, 10L).status());
    }

    @Test
    void cannotMarkReturnedWithoutApprovedClaim() {
        item.setStatus(ItemStatus.CLAIM_PENDING);

        assertThrows(ConflictException.class, () -> claimService.markItemReturned(1L, 10L));
    }

    @Test
    void strangerCannotMarkReturned() {
        item.setStatus(ItemStatus.CLAIM_PENDING);
        when(claimRepository.existsByItemIdAndStatus(10L, ClaimStatus.APPROVED)).thenReturn(true);

        assertThrows(ForbiddenException.class, () -> claimService.markItemReturned(4L, 10L));
    }
}
