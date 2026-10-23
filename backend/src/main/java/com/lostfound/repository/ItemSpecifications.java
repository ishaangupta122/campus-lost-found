package com.lostfound.repository;

import java.util.Collection;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import com.lostfound.model.Item;
import com.lostfound.model.ItemStatus;
import com.lostfound.model.ItemType;

/** Builds the search query; a condition is added only for each filter supplied. */
public final class ItemSpecifications {

    private ItemSpecifications() {
    }

    public static Specification<Item> search(String keyword, ItemType type, String category,
                                             String location, Collection<ItemStatus> statuses) {
        Specification<Item> spec = (root, query, cb) -> root.get("status").in(statuses);

        if (type != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("type"), type));
        }
        if (StringUtils.hasText(category)) {
            String wanted = category.trim().toLowerCase();
            spec = spec.and((root, query, cb) -> cb.equal(cb.lower(root.get("category")), wanted));
        }
        if (StringUtils.hasText(location)) {
            String pattern = "%" + location.trim().toLowerCase() + "%";
            spec = spec.and((root, query, cb) -> cb.like(cb.lower(root.get("location")), pattern));
        }
        if (StringUtils.hasText(keyword)) {
            String pattern = "%" + keyword.trim().toLowerCase() + "%";
            spec = spec.and((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("itemName")), pattern),
                    cb.like(cb.lower(root.get("description")), pattern)));
        }
        return spec;
    }
}
