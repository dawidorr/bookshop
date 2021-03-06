package com.example.bookaro.catalog.application;

import com.example.bookaro.catalog.application.port.CatalogUseCase;
import com.example.bookaro.catalog.db.AuthorJpaRepository;
import com.example.bookaro.catalog.db.BookJpaRepository;
import com.example.bookaro.catalog.domain.Author;
import com.example.bookaro.catalog.domain.Book;
import com.example.bookaro.uploads.application.ports.UploadUseCase;
import com.example.bookaro.uploads.domain.Upload;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
class CatalogService implements CatalogUseCase {


    private final BookJpaRepository repository;
    private final UploadUseCase upload;
    private final AuthorJpaRepository authorRepository;


    @Override
    public List<Book> findAll() {
        return repository.findAll();
    }

    @Override
    public Optional<Book> findById(Long id) {
        return repository.findById(id);
    }


    @Override
    public List<Book> findByTitle(String title) {
        return repository.findAll()
                .stream()
                .filter(book -> book.getTitle().toLowerCase().contains(title.toLowerCase()))
                .collect(Collectors.toList());
    }

    @Override
    public Optional<Book> findOneByTitle(String title) {
        return repository.findAll()
                .stream()
                .filter(book -> book.getTitle().toLowerCase().contains(title.toLowerCase()))
                .findFirst();
    }

    @Override
    public Optional<Book> findOneByAuthor(String author) {
        return repository.findAll()
                .stream()
//                .filter(book -> book.getAuthor().toLowerCase().contains(author.toLowerCase()))
                .findFirst();
    }


    @Override
    public List<Book> findByAuthor(String author) {
        return repository
                .findByAuthor(author);

    }



    @Override
    public List<Book> findByTitleAndAuthor(String title, String author) {
        return repository.findAll()
                .stream()
                .filter(book -> book.getTitle().toLowerCase().contains(title.toLowerCase()))
//                .filter(book -> book.getAuthor().toLowerCase().contains(author.toLowerCase()))
                .collect(Collectors.toList());
    }

    @Override
    public Book addBook(CreateBookCommand command) {
        Book book = toBook(command);
        return repository.save(book);
    }

    private Book toBook(CreateBookCommand command) {
        Book book = new Book(command.getTitle(), command.getYear(), command.getPrice());
        Set<Author> authors = fetchAuthorsByIds(command.getAuthors());
        book.setAuthors(authors);
        return book;
    }

    private Set<Author> fetchAuthorsByIds(Set<Long> authors) {
        return authors
                .stream()
                .map(authorId -> authorRepository
                        .findById(authorId)
                        .orElseThrow(() -> new IllegalArgumentException("Unable to find author with id: " + authorId))
                )
                .collect(Collectors.toSet());
    }

    @Override
    public UpdateBookResponse updateBook(UpdateBookCommand command) {
        return repository
                .findById(command.getId())
                .map(book -> {
                    Book updatedBook = updateFields(command,book);
                    repository.save(updatedBook);
                    return UpdateBookResponse.SUCCESS;
                })
                .orElseGet(() -> new UpdateBookResponse(false, Collections.singletonList("Book not found with id: " + command.getId())));

    }

    private Book updateFields(UpdateBookCommand command, Book book ){
            if (command.getTitle() != null) {
                book.setTitle(command.getTitle());
            }
            if (command.getAuthors() != null && command.getAuthors().size() > 0) {
                book.setAuthors(fetchAuthorsByIds(command.getAuthors()));
            }
            if (command.getYear() != null) {
                book.setYear(command.getYear());
            }
            if(command.getPrice() != null){
                book.setPrice(command.getPrice());
            }
            return book;

    }

    @Override
    public void removeById(Long id) {
        repository.deleteById(id);

    }

    @Override
    public void updateBookCover(UpdateBookCoverCommand command) {
        repository.findById(command.getId())
                .ifPresent(book -> {
                    Upload savedUpload = upload.save(new UploadUseCase.SaveUploadCommand(command.getFilename(), command.getFile(), command.getContentType()));
                    book.setCoverId(savedUpload.getId());
                    repository.save(book);
                });
    }

    @Override
    public void removeBookCover(Long id) {
        repository.findById(id)
                .ifPresent(book -> {
                    if (book.getCoverId() != null) {

                        upload.removeById(book.getCoverId());
                        book.setCoverId(null);
                        repository.save(book);

                    }
                });

    }


}