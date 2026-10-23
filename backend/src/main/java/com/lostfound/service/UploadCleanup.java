package com.lostfound.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.lostfound.model.Item;
import com.lostfound.model.ItemStatus;
import com.lostfound.repository.ItemRepository;

/**
 * Nightly (03:00) cleanup: deletes unused uploads, and pictures of RETURNED/CLOSED reports
 * older than 30 days.
 */
@Component
public class UploadCleanup {

    private static final Logger log = LoggerFactory.getLogger(UploadCleanup.class);
    private static final Duration UNUSED_UPLOAD_MIN_AGE = Duration.ofHours(24);
    private static final long FINISHED_REPORT_RETENTION_DAYS = 30;

    private final ImageStorageService imageStorageService;
    private final ItemRepository itemRepository;

    public UploadCleanup(ImageStorageService imageStorageService, ItemRepository itemRepository) {
        this.imageStorageService = imageStorageService;
        this.itemRepository = itemRepository;
    }

    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void run() {
        int expired = removePicturesOfFinishedItems();
        int orphans = imageStorageService.deleteOrphans(itemRepository::existsByImagePath, UNUSED_UPLOAD_MIN_AGE);
        log.info("Upload cleanup: {} pictures of finished reports and {} unused uploads deleted", expired, orphans);
    }

    private int removePicturesOfFinishedItems() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(FINISHED_REPORT_RETENTION_DAYS);
        List<Item> finished = itemRepository.findByStatusInAndCreatedAtBeforeAndImagePathIsNotNull(
                List.of(ItemStatus.RETURNED, ItemStatus.CLOSED), cutoff);
        for (Item item : finished) {
            imageStorageService.deleteByImagePath(item.getImagePath());
            item.setImagePath(null);
        }
        return finished.size();
    }
}
