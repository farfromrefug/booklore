package com.adityachandel.booklore.service.metadata;

import com.adityachandel.booklore.exception.ApiError;
import com.adityachandel.booklore.mapper.BookMapper;
import com.adityachandel.booklore.mapper.BookMetadataMapper;
import com.adityachandel.booklore.mapper.MetadataClearFlagsMapper;
import com.adityachandel.booklore.model.MetadataClearFlags;
import com.adityachandel.booklore.model.MetadataUpdateContext;
import com.adityachandel.booklore.model.MetadataUpdateWrapper;
import com.adityachandel.booklore.model.dto.Book;
import com.adityachandel.booklore.model.dto.BookMetadata;
import com.adityachandel.booklore.model.dto.request.BulkMetadataUpdateRequest;
import com.adityachandel.booklore.model.dto.request.FetchMetadataRequest;
import com.adityachandel.booklore.model.dto.request.ToggleAllLockRequest;
import com.adityachandel.booklore.model.entity.BookEntity;
import com.adityachandel.booklore.model.entity.BookMetadataEntity;
import com.adityachandel.booklore.model.enums.BookFileType;
import com.adityachandel.booklore.model.enums.Lock;
import com.adityachandel.booklore.model.enums.MetadataProvider;
import com.adityachandel.booklore.model.websocket.Topic;
import com.adityachandel.booklore.repository.BookMetadataRepository;
import com.adityachandel.booklore.repository.BookRepository;
import com.adityachandel.booklore.service.NotificationService;
import com.adityachandel.booklore.service.book.BookQueryService;
import com.adityachandel.booklore.service.metadata.extractor.CbxMetadataExtractor;
import com.adityachandel.booklore.service.metadata.parser.BookParser;
import com.adityachandel.booklore.util.FileUtils;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.File;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@AllArgsConstructor
public class BookMetadataService {

    private final BookRepository bookRepository;
    private final BookMapper bookMapper;
    private final BookMetadataMapper bookMetadataMapper;
    private final BookMetadataUpdater bookMetadataUpdater;
    private final NotificationService notificationService;
    private final BookMetadataRepository bookMetadataRepository;
    private final BookQueryService bookQueryService;
    private final Map<MetadataProvider, BookParser> parserMap;
    private final CbxMetadataExtractor cbxMetadataExtractor;
    private final MetadataClearFlagsMapper metadataClearFlagsMapper;
    private final com.adityachandel.booklore.service.book.BookCreatorService bookCreatorService;


    public Flux<BookMetadata> getProspectiveMetadataListForBookId(long bookId, FetchMetadataRequest request) {
        BookEntity bookEntity = bookRepository.findById(bookId).orElseThrow(() -> ApiError.BOOK_NOT_FOUND.createException(bookId));
        Book book = bookMapper.toBook(bookEntity);

        return Flux.fromIterable(request.getProviders())
                .flatMap(provider ->
                    Mono.fromCallable(() -> fetchMetadataListFromAProvider(provider, book, request))
                            .subscribeOn(Schedulers.boundedElastic())
                            .flatMapMany(Flux::fromIterable)
                            .onErrorResume(e -> {
                                log.error("Error fetching metadata from provider: {}", provider, e);
                                return Flux.empty();
                            })
                );
    }

    public List<BookMetadata> fetchMetadataListFromAProvider(MetadataProvider provider, Book book, FetchMetadataRequest request) {
        return getParser(provider).fetchMetadata(book, request);
    }


    private BookParser getParser(MetadataProvider provider) {
        BookParser parser = parserMap.get(provider);
        if (parser == null) {
            throw ApiError.METADATA_SOURCE_NOT_IMPLEMENT_OR_DOES_NOT_EXIST.createException();
        }
        return parser;
    }

    public void toggleFieldLocks(List<Long> bookIds, Map<String, String> fieldActions) {
        Map<String, String> fieldMapping = Map.of(
                "thumbnailLocked", "coverLocked"
        );
        List<BookMetadataEntity> metadataEntities = bookMetadataRepository
                .getMetadataForBookIds(bookIds)
                .stream()
                .distinct()
                .toList();

        for (BookMetadataEntity metadataEntity : metadataEntities) {
            fieldActions.forEach((field, action) -> {
                String entityField = fieldMapping.getOrDefault(field, field);
                try {
                    String setterName = "set" + Character.toUpperCase(entityField.charAt(0)) + entityField.substring(1);
                    Method setter = BookMetadataEntity.class.getMethod(setterName, Boolean.class);
                    setter.invoke(metadataEntity, "LOCK".equalsIgnoreCase(action));
                } catch (Exception e) {
                    throw new RuntimeException("Failed to invoke setter for field: " + entityField + " on bookId: " + metadataEntity.getBookId(), e);
                }
            });
        }

        bookMetadataRepository.saveAll(metadataEntities);
    }

    @Transactional
    public List<BookMetadata> toggleAllLock(ToggleAllLockRequest request) {
        boolean lock = request.getLock() == Lock.LOCK;
        List<BookEntity> books = bookQueryService.findAllWithMetadataByIds(request.getBookIds())
                .stream()
                .peek(book -> book.getMetadata().applyLockToAllFields(lock))
                .toList();
        bookRepository.saveAll(books);
        return books.stream().map(b -> bookMetadataMapper.toBookMetadata(b.getMetadata(), false)).collect(Collectors.toList());
    }

    public BookMetadata getComicInfoMetadata(long bookId) {
        log.info("Extracting ComicInfo metadata for book ID: {}", bookId);
        BookEntity bookEntity = bookRepository.findById(bookId).orElseThrow(() -> ApiError.BOOK_NOT_FOUND.createException(bookId));
        var primaryFile = bookEntity.getPrimaryBookFile();
        if (primaryFile == null || primaryFile.getBookType() != BookFileType.CBX) {
            log.info("Unsupported operation for book ID {} - no file or not CBX type", bookId);
            return null;
        }
        return cbxMetadataExtractor.extractMetadata(new File(FileUtils.getBookFullPath(bookEntity)));
    }

    @Transactional
    public void bulkUpdateMetadata(BulkMetadataUpdateRequest request, boolean mergeCategories, boolean mergeMoods, boolean mergeTags) {
        List<BookEntity> books = bookRepository.findAllWithMetadataByIds(request.getBookIds());

        MetadataClearFlags clearFlags = metadataClearFlagsMapper.toClearFlags(request);

        for (BookEntity book : books) {
            BookMetadata bookMetadata = BookMetadata.builder()
                    .authors(request.getAuthors())
                    .publisher(request.getPublisher())
                    .language(request.getLanguage())
                    .seriesName(request.getSeriesName())
                    .seriesTotal(request.getSeriesTotal())
                    .publishedDate(request.getPublishedDate())
                    .categories(request.getGenres())
                    .moods(request.getMoods())
                    .tags(request.getTags())
                    .build();

            MetadataUpdateContext context = MetadataUpdateContext.builder()
                    .bookEntity(book)
                    .metadataUpdateWrapper(MetadataUpdateWrapper.builder()
                            .metadata(bookMetadata)
                            .clearFlags(clearFlags)
                            .build())
                    .updateThumbnail(false)
                    .mergeCategories(mergeCategories)
                    .mergeMoods(mergeMoods)
                    .mergeTags(mergeTags)
                    .build();

            bookMetadataUpdater.setBookMetadata(context);
            notificationService.sendMessage(Topic.BOOK_UPDATE, bookMapper.toBook(book));
        }
    }

    /**
     * Reload metadata from CBX file for a single book.
     * Extracts metadata from ComicInfo.xml and updates the book entity.
     */
    @Transactional
    public void reloadMetadataFromFile(long bookId) {
        BookEntity bookEntity = bookRepository.findById(bookId).orElseThrow(() -> ApiError.BOOK_NOT_FOUND.createException(bookId));
        
        if (bookEntity.getPrimaryBookFile().getBookType() != BookFileType.CBX) {
            throw ApiError.INVALID_INPUT.createException("Only CBX files support metadata reload from file");
        }
        
        log.info("Reloading metadata from file for book ID: {}", bookId);
        
        try {
            BookMetadata extracted = cbxMetadataExtractor.extractMetadata(new File(FileUtils.getBookFullPath(bookEntity)));
            if (extracted != null) {
                BookMetadataEntity metadata = bookEntity.getMetadata();
                
                // Update metadata fields if not locked
                if (!isFieldLocked(metadata.getTitleLocked())) {
                    metadata.setTitle(truncate(extracted.getTitle(), 1000));
                }
                if (!isFieldLocked(metadata.getDescriptionLocked())) {
                    metadata.setDescription(truncate(extracted.getDescription(), 5000));
                }
                if (!isFieldLocked(metadata.getPublisherLocked())) {
                    metadata.setPublisher(truncate(extracted.getPublisher(), 1000));
                }
                if (!isFieldLocked(metadata.getPublishedDateLocked())) {
                    metadata.setPublishedDate(extracted.getPublishedDate());
                }
                if (!isFieldLocked(metadata.getSeriesNameLocked())) {
                    metadata.setSeriesName(truncate(extracted.getSeriesName(), 1000));
                }
                if (!isFieldLocked(metadata.getSeriesNumberLocked())) {
                    metadata.setSeriesNumber(extracted.getSeriesNumber());
                }
                if (!isFieldLocked(metadata.getSeriesTotalLocked())) {
                    metadata.setSeriesTotal(extracted.getSeriesTotal());
                }
                if (!isFieldLocked(metadata.getPageCountLocked())) {
                    metadata.setPageCount(extracted.getPageCount());
                }
                if (!isFieldLocked(metadata.getLanguageLocked())) {
                    metadata.setLanguage(truncate(extracted.getLanguage(), 1000));
                }
                
                bookRepository.save(bookEntity);
                
                // Handle authors and categories separately after save
                // Clear and reload only if not locked and new data is available
                if (!isFieldLocked(metadata.getAuthorsLocked()) && extracted.getAuthors() != null && !extracted.getAuthors().isEmpty()) {
                    metadata.getAuthors().clear();
                    bookRepository.save(bookEntity);
                    bookCreatorService.addAuthorsToBook(extracted.getAuthors(), bookEntity);
                }
                
                if (!isFieldLocked(metadata.getCategoriesLocked()) && extracted.getCategories() != null && !extracted.getCategories().isEmpty()) {
                    metadata.getCategories().clear();
                    bookRepository.save(bookEntity);
                    bookCreatorService.addCategoriesToBook(extracted.getCategories(), bookEntity);
                }
                
                bookRepository.save(bookEntity);
                notificationService.sendMessage(Topic.BOOK_UPDATE, bookMapper.toBook(bookEntity));
                log.info("Successfully reloaded metadata from file for book ID: {} page count:{}", bookId, extracted.getPageCount());
            } else {
                log.warn("No metadata could be extracted from file for book ID: {}", bookId);
            }
        } catch (Exception e) {
            log.error("Failed to reload metadata from file for book ID {}: {}", bookId, e.getMessage(), e);
            throw ApiError.INVALID_INPUT.createException("Failed to reload metadata: " + e.getMessage());
        }
    }
    
    /**
     * Reload metadata from CBX files for multiple books.
     */
    public void reloadMetadataFromFileForBooks(Set<Long> bookIds) {
        List<BookEntity> books = bookQueryService.findAllWithMetadataByIds(bookIds).stream()
                .filter(book -> book.getPrimaryBookFile().getBookType() == BookFileType.CBX)
                .toList();
        
        log.info("Starting metadata reload from files for {} books", books.size());
        
        for (BookEntity book : books) {
            try {
                reloadMetadataFromFile(book.getId());
            } catch (Exception e) {
                log.error("Failed to reload metadata for book ID {}: {}", book.getId(), e.getMessage());
            }
        }
        
        log.info("Completed metadata reload from files");
    }
    
    private boolean isFieldLocked(Boolean locked) {
        return locked != null && locked;
    }
    
    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
