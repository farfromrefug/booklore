package com.adityachandel.booklore.controller;

import com.adityachandel.booklore.config.JacksonConfig;
import com.adityachandel.booklore.mapper.komga.KomgaMapper;
import com.adityachandel.booklore.model.dto.komga.*;
import com.adityachandel.booklore.service.book.BookService;
import com.adityachandel.booklore.service.komga.KomgaService;
import com.adityachandel.booklore.service.opds.OpdsUserV2Service;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Komga API", description = "Komga-compatible API endpoints. " +
        "All endpoints support a 'clean' query parameter (default: false). " +
        "When present (?clean or ?clean=true), responses exclude fields ending with 'Lock', null values, and empty arrays, " +
        "resulting in smaller and cleaner JSON payloads.")
@Slf4j
@RestController
@RequestMapping(value = "/komga/api", produces = "application/json")
@RequiredArgsConstructor
public class KomgaController {

    private final KomgaService komgaService;
    private final BookService bookService;
    private final OpdsUserV2Service opdsUserV2Service;
    private final KomgaMapper komgaMapper;

    // Inject the dedicated komga mapper bean
    private final @Qualifier(JacksonConfig.KOMGA_CLEAN_OBJECT_MAPPER) ObjectMapper komgaCleanObjectMapper;

    // Helper to serialize using the komga-clean mapper
    private ResponseEntity<String> writeJson(Object body) {
        try {
            String json = komgaCleanObjectMapper.writeValueAsString(body);
            return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(json);
        } catch (Exception e) {
            log.error("Failed to serialize Komga response", e);
            return ResponseEntity.status(500).build();
        }
    }

    // ==================== Libraries ====================
    
    @Operation(summary = "List all libraries")
    @GetMapping("/v1/libraries")
    public ResponseEntity<String> getAllLibraries() {
        List<KomgaLibraryDto> libraries = komgaService.getAllLibraries();
        return writeJson(libraries);
    }

    @Operation(summary = "Get library details")
    @GetMapping("/v1/libraries/{libraryId}")
    public ResponseEntity<String> getLibrary(
            @Parameter(description = "Library ID") @PathVariable Long libraryId) {
        return writeJson(komgaService.getLibraryById(libraryId));
    }

    // ==================== Series ====================
    
    @Operation(summary = "List series")
    @GetMapping("/v1/series")
    public ResponseEntity<String> getAllSeries(
            @Parameter(description = "Library ID filter") @RequestParam(required = false, name = "library_id") Long libraryId,
            @Parameter(description = "Page number") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Return all books without paging") @RequestParam(defaultValue = "false") boolean unpaged) {
        KomgaPageableDto<KomgaSeriesDto> result = komgaService.getAllSeries(libraryId, page, size, unpaged);
        return writeJson(result);
    }

    @Operation(summary = "Get series details")
    @GetMapping("/v1/series/{seriesId}")
    public ResponseEntity<String> getSeries(
            @Parameter(description = "Series ID") @PathVariable String seriesId)  {
        return writeJson(komgaService.getSeriesById(seriesId));
    }

    @Operation(summary = "List books in series")
    @GetMapping("/v1/series/{seriesId}/books")
    public ResponseEntity<String> getSeriesBooks(
            @Parameter(description = "Series ID") @PathVariable String seriesId,
            @Parameter(description = "Page number") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Return all books without paging") @RequestParam(defaultValue = "false") boolean unpaged) {
        return writeJson(komgaService.getBooksBySeries(seriesId, page, size, unpaged));
    }

    @Operation(summary = "Get series thumbnail")
    @GetMapping("/v1/series/{seriesId}/thumbnail")
    public ResponseEntity<Resource> getSeriesThumbnail(
            @Parameter(description = "Series ID") @PathVariable String seriesId) {
        KomgaPageableDto<KomgaBookDto> books = komgaService.getBooksBySeries(seriesId, 0, 1, false);
        if (books.getContent().isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        Long firstBookId = Long.parseLong(books.getContent().get(0).getId());
        Resource coverImage = bookService.getBookThumbnail(firstBookId);
        return ResponseEntity.ok()
                .header("Content-Type", "image/jpeg")
                .body(coverImage);
    }

    // ==================== Books ====================
    
    @Operation(summary = "List books")
    @GetMapping("/v1/books")
    public ResponseEntity<String> getAllBooks(
            @Parameter(description = "Library ID filter") @RequestParam(required = false, name = "library_id") Long libraryId,
            @Parameter(description = "Page number") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size) {
        KomgaPageableDto<KomgaBookDto> result = komgaService.getAllBooks(libraryId, page, size);
        return writeJson(result);
    }

    @Operation(summary = "Get book details")
    @GetMapping("/v1/books/{bookId}")
    public ResponseEntity<String> getBook(
            @Parameter(description = "Book ID") @PathVariable Long bookId) {
        return writeJson(komgaService.getBookById(bookId));
    }

    @Operation(summary = "Get book pages metadata")
    @GetMapping("/v1/books/{bookId}/pages")
    public ResponseEntity<String> getBookPages(
            @Parameter(description = "Book ID") @PathVariable Long bookId) {
        return writeJson(komgaService.getBookPages(bookId));
    }

    @Operation(summary = "Get book page image")
    @GetMapping("/v1/books/{bookId}/pages/{pageNumber}")
    public ResponseEntity<Resource> getBookPage(
            @Parameter(description = "Book ID") @PathVariable Long bookId,
            @Parameter(description = "Page number") @PathVariable Integer pageNumber,
            @Parameter(description = "Convert image format (e.g., 'png')") @RequestParam(required = false) String convert) {
        try {
            boolean convertToPng = "png".equalsIgnoreCase(convert);
            Resource pageImage = komgaService.getBookPageImage(bookId, pageNumber, convertToPng);
            // Note: When not converting, we assume JPEG as most CBZ files contain JPEG images,
            // but the actual format may vary (PNG, WebP, etc.)
            String contentType = convertToPng ? "image/png" : "image/jpeg";
            return ResponseEntity.ok()
                    .header("Content-Type", contentType)
                    .body(pageImage);
        } catch (Exception e) {
            log.error("Failed to get page {} from book {}", pageNumber, bookId, e);
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(summary = "Download book file")
    @GetMapping("/v1/books/{bookId}/file")
    public ResponseEntity<Resource> downloadBook(
            @Parameter(description = "Book ID") @PathVariable Long bookId) {
        return bookService.downloadBook(bookId);
    }

    @Operation(summary = "Get book thumbnail")
    @GetMapping("/v1/books/{bookId}/thumbnail")
    public ResponseEntity<Resource> getBookThumbnail(
            @Parameter(description = "Book ID") @PathVariable Long bookId) {
        Resource coverImage = bookService.getBookThumbnail(bookId);
        return ResponseEntity.ok()
                .header("Content-Type", "image/jpeg")
                .body(coverImage);
    }

    // ==================== Users ====================
    
    @Operation(summary = "Get current user details")
    @GetMapping("/v2/users/me")
    public ResponseEntity<String> getCurrentUser(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            return ResponseEntity.status(401).build();
        }
        
        String username = authentication.getName();
        var opdsUser = opdsUserV2Service.findByUsername(username);
        
        if (opdsUser == null) {
            return ResponseEntity.notFound().build();
        }
        
        return writeJson(komgaMapper.toKomgaUserDto(opdsUser));
    }
    
    // ==================== Collections ====================
    
    @Operation(summary = "List collections")
    @GetMapping("/v1/collections")
    public ResponseEntity<String> getCollections(
            Authentication authentication,
            @Parameter(description = "Page number") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Return all collections without paging") @RequestParam(defaultValue = "false") boolean unpaged) {
        
        // Get OPDS user from authentication
        Long userId = null;
        if (authentication != null && authentication.getName() != null) {
            String username = authentication.getName();
            var opdsUser = opdsUserV2Service.findByUsername(username);
            if (opdsUser != null) {
                userId = opdsUser.getUser().getId();
            }
        }
        
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }
        
        return writeJson(komgaService.getCollections(userId, page, size, unpaged));
    }
    
    // ==================== Genres ====================
    
    @Operation(summary = "List genres")
    @GetMapping("/v1/genres")
    public ResponseEntity<String> getGenres(
            @Parameter(description = "Library ID filter") @RequestParam(name = "library_id", required = false) List<Long> libraryIds,
            @Parameter(description = "Collection ID filter") @RequestParam(name = "collection_id", required = false) Long collectionId) {
        return writeJson(komgaService.getGenres(libraryIds, collectionId));
    }
    
    // ==================== Tags ====================
    
    @Operation(summary = "List tags")
    @GetMapping("/v1/tags")
    public ResponseEntity<String> getTags(
            @Parameter(description = "Library ID filter") @RequestParam(name = "library_id", required = false) List<Long> libraryIds,
            @Parameter(description = "Collection ID filter") @RequestParam(name = "collection_id", required = false) Long collectionId) {
        return writeJson(komgaService.getTags(libraryIds, collectionId));
    }
    
    // ==================== Publishers ====================
    
    @Operation(summary = "List publishers")
    @GetMapping("/v1/publishers")
    public ResponseEntity<String> getPublishers(
            @Parameter(description = "Library ID filter") @RequestParam(name = "library_id", required = false) List<Long> libraryIds,
            @Parameter(description = "Collection ID filter") @RequestParam(name = "collection_id", required = false) Long collectionId) {
        return writeJson(komgaService.getPublishers(libraryIds, collectionId));
    }
    
    // ==================== Authors ====================
    
    @Operation(summary = "List authors")
    @GetMapping("/v1/authors")
    public ResponseEntity<String> getAuthors(
            @Parameter(description = "Search query") @RequestParam(required = false) String search,
            @Parameter(description = "Author role filter") @RequestParam(required = false) String role,
            @Parameter(description = "Library ID filter") @RequestParam(name = "library_id", required = false) List<Long> libraryIds,
            @Parameter(description = "Collection ID filter") @RequestParam(name = "collection_id", required = false) Long collectionId,
            @Parameter(description = "Series ID filter") @RequestParam(name = "series_id", required = false) String seriesId,
            @Parameter(description = "Readlist ID filter") @RequestParam(name = "readlist_id", required = false) Long readlistId,
            @Parameter(description = "Page number") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Return all authors without paging") @RequestParam(defaultValue = "true") boolean unpaged) {

        KomgaPageableDto<KomgaAuthorDto> result = komgaService.getAuthors(search, role, libraryIds, collectionId, seriesId, readlistId, page, size, unpaged);
        // The komga API does not returned paged content
        // which can be slow. So for unpaged we return it as Komga API expects it.
        // otherwise we return it paged.
        if (unpaged) {
            return writeJson(result.getContent());
        } else {
            return writeJson(result);
        }
    }
}