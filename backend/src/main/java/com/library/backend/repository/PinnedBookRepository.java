package com.library.backend.repository;

import com.library.backend.entity.PinnedBook;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PinnedBookRepository extends JpaRepository<PinnedBook, Long> {

    boolean existsByMemberIdAndBookIsbn(Long memberId, String isbn);

    void deleteByMemberIdAndBookIsbn(Long memberId, String isbn);

    void deleteByBookIsbn(String isbn);

    long countByMemberId(Long memberId);

    // Fetch the book up front: getPinnedBooks reads book fields for every pin.
    @EntityGraph(attributePaths = "book")
    List<PinnedBook> findAllByMemberIdOrderByPinnedAtDesc(Long memberId);
}
