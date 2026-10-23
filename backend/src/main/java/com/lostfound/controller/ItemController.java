package com.lostfound.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.lostfound.dto.ItemRequest;
import com.lostfound.dto.ItemResponse;
import com.lostfound.model.ItemStatus;
import com.lostfound.model.ItemType;
import com.lostfound.service.ItemService;

import jakarta.validation.Valid;

/** Item endpoints. The caller comes from the verified JWT, never from the request body. */
@RestController
@RequestMapping("/api/items")
public class ItemController {

    private final ItemService itemService;

    public ItemController(ItemService itemService) {
        this.itemService = itemService;
    }

    @PostMapping
    public ResponseEntity<ItemResponse> create(Authentication authentication,
                                               @Valid @RequestBody ItemRequest request) {
        ItemResponse created = itemService.createItem(CurrentUser.id(authentication), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping
    public List<ItemResponse> list(@RequestParam(required = false) String q,
                                   @RequestParam(required = false) ItemType type,
                                   @RequestParam(required = false) String category,
                                   @RequestParam(required = false) String location,
                                   @RequestParam(required = false) ItemStatus status) {
        return itemService.searchItems(q, type, category, location, status);
    }

    // fixed path wins over "/{id}"
    @GetMapping("/my")
    public List<ItemResponse> myItems(Authentication authentication) {
        return itemService.getMyItems(CurrentUser.id(authentication));
    }

    @GetMapping("/{id}")
    public ItemResponse getById(@PathVariable Long id) {
        return itemService.getItem(id);
    }

    @PutMapping("/{id}")
    public ItemResponse update(Authentication authentication,
                               @PathVariable Long id,
                               @Valid @RequestBody ItemRequest request) {
        return itemService.updateItem(CurrentUser.id(authentication), id, request);
    }

    @PutMapping("/{id}/close")
    public ItemResponse close(Authentication authentication, @PathVariable Long id) {
        return itemService.closeItem(CurrentUser.id(authentication), id);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(Authentication authentication, @PathVariable Long id) {
        itemService.deleteItem(CurrentUser.id(authentication), id);
        return ResponseEntity.noContent().build();
    }
}
