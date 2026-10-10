package com.campus.lostandfound.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

import com.campus.lostandfound.model.Item;
import com.campus.lostandfound.model.ItemStatus;
import com.campus.lostandfound.model.ItemType;

/** JpaSpecificationExecutor adds findAll(Specification, Sort) used for search and filters. */
public interface ItemRepository extends JpaRepository<Item, Long>, JpaSpecificationExecutor<Item> {

    /** Reports made by one user, newest first (GET /api/items/my). */
    List<Item> findByReporterIdOrderByCreatedAtDesc(Long reporterId);

    /** Items whose status is in the given list, e.g. all active ones. */
    List<Item> findByStatusInOrderByCreatedAtDesc(Collection<ItemStatus> statuses);

    /**
     * Loads the item and LOCKS its database row until the current transaction ends.
     * Two people claiming (or two admins deciding) at the same moment are handled one
     * after the other, so an item can never end up with two approved claims.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from Item i where i.id = :id")
    Optional<Item> findByIdForUpdate(@Param("id") Long id);

    /** Used by the upload cleanup: is this picture still used by a report? */
    boolean existsByImagePath(String imagePath);

    /** Used by the upload cleanup: finished reports older than a date that still have a picture. */
    List<Item> findByStatusInAndCreatedAtBeforeAndImagePathIsNotNull(Collection<ItemStatus> statuses, java.time.LocalDateTime before);

    // Counts for the admin dashboard.
    long countByType(ItemType type);

    long countByStatus(ItemStatus status);
}
