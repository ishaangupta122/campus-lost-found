package com.campus.lostandfound.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.campus.lostandfound.model.Item;
import com.campus.lostandfound.model.ItemStatus;
import com.campus.lostandfound.repository.ItemRepository;

/**
 * Keeps the upload folder from growing forever. Runs every night at 03:00 and does two things:
 *  1. deletes pictures that no report uses (an upload that was never saved with a report);
 *  2. deletes the picture of reports that are finished (RETURNED or CLOSED) and older than
 *     app.upload.retention-days. The report stays; only its picture is removed.
 *     Set the number to 0 to keep pictures of finished reports forever.
 */
@Component
public class UploadCleanup {

    private static final Logger log = LoggerFactory.getLogger(UploadCleanup.class);

    private final ImageStorageService imageStorageService;
    private final ItemRepository itemRepository;
    private final Duration orphanMinAge;
    private final long retentionDays;

    public UploadCleanup(ImageStorageService imageStorageService,
                         ItemRepository itemRepository,
                         @Value("${app.upload.orphan-max-age-hours}") long orphanMaxAgeHours,
                         @Value("${app.upload.retention-days}") long retentionDays) {
        this.imageStorageService = imageStorageService;
        this.itemRepository = itemRepository;
        this.orphanMinAge = Duration.ofHours(orphanMaxAgeHours);
        this.retentionDays = retentionDays;
    }

    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void run() {
        int expired = removePicturesOfFinishedItems();
        int orphans = imageStorageService.deleteOrphans(itemRepository::existsByImagePath, orphanMinAge);
        log.info("Upload cleanup: {} pictures of finished reports and {} unused uploads deleted", expired, orphans);
    }

    private int removePicturesOfFinishedItems() {
        if (retentionDays <= 0) {
            return 0;
        }
        LocalDateTime cutoff = LocalDateTime.now().minusDays(retentionDays);
        List<Item> finished = itemRepository.findByStatusInAndCreatedAtBeforeAndImagePathIsNotNull(
                List.of(ItemStatus.RETURNED, ItemStatus.CLOSED), cutoff);
        for (Item item : finished) {
            imageStorageService.deleteByImagePath(item.getImagePath());
            item.setImagePath(null);
        }
        return finished.size();
    }
}
