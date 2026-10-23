package com.lostfound.repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

import com.lostfound.model.Item;
import com.lostfound.model.ItemStatus;
import com.lostfound.model.ItemType;

/** JpaSpecificationExecutor adds findAll(Specification, Sort) for search. */
public interface ItemRepository extends JpaRepository<Item, Long>, JpaSpecificationExecutor<Item> {

    /** Reports by one user, newest first. */
    List<Item> findByReporterIdOrderByCreatedAtDesc(Long reporterId);

    /** Loads the item and locks its row until the transaction ends, so simultaneous decisions run one after the other. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from Item i where i.id = :id")
    Optional<Item> findByIdForUpdate(@Param("id") Long id);

    /** Upload cleanup: is the picture still used? */
    boolean existsByImagePath(String imagePath);

    /** Upload cleanup: old finished reports that still have a picture. */
    List<Item> findByStatusInAndCreatedAtBeforeAndImagePathIsNotNull(Collection<ItemStatus> statuses, LocalDateTime before);

    // Counts for the admin dashboard.
    long countByType(ItemType type);

    long countByStatus(ItemStatus status);
}
