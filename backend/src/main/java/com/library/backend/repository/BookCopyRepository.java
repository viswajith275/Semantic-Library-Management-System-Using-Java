package com.library.backend.repository;

import com.library.backend.entity.BookCopy;
import com.library.backend.entity.CopyStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BookCopyRepository extends JpaRepository<BookCopy, String> {

    Optional<BookCopy> findByBarcode(String barcode);

    boolean existsByBarcode(String barcode);

    List<BookCopy> findAllByBookIsbn(String isbn);

    long countByStatus(CopyStatus status);

    void deleteAllByBookIsbn(String isbn);
}
