package com.campus.lostandfound.dto;

/**
 * Result of an image upload. The client puts imagePath into the item form
 * (the "imagePath" field of a report) and shows the picture at that URL.
 */
public record UploadResponse(String imagePath) {
}
