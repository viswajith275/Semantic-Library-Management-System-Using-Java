package com.library.backend;

import com.library.backend.dto.LoanDto.ExtendLoanRequest;
import com.library.backend.entity.Book;
import com.library.backend.entity.BookCopy;
import com.library.backend.entity.CopyStatus;
import com.library.backend.entity.LoanRecord;
import com.library.backend.entity.Role;
import com.library.backend.entity.User;
import com.library.backend.repository.BookCopyRepository;
import com.library.backend.repository.BookRepository;
import com.library.backend.repository.LoanRecordRepository;
import com.library.backend.repository.UserRepository;
import com.library.backend.service.CirculationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/** The overdue branch of extendLoan: an overdue loan must restart from today, not stay in the past. */
@SpringBootTest
class LoanExtensionTests {

    @Autowired private CirculationService circulationService;
    @Autowired private BookRepository bookRepository;
    @Autowired private BookCopyRepository bookCopyRepository;
    @Autowired private LoanRecordRepository loanRecordRepository;
    @Autowired private UserRepository userRepository;

    @Test
    void extendingAnOverdueLoanStartsFromToday() {
        Book book = bookRepository.save(new Book(
            "9780000000099", "Overdue Test", "Test Author", "#000000", null, null, "Test Shelf", null));
        BookCopy copy = bookCopyRepository.save(new BookCopy("BC-OVERDUE-01", book, CopyStatus.AVAILABLE));

        User member = new User();
        member.setName("Overdue Member");
        member.setRole(Role.MEMBER);
        member.setActive(true);
        member.setCustomCode("MEM-99999999");
        member = userRepository.save(member);

        LocalDateTime borrowedAt = LocalDateTime.now().minusDays(14);
        LocalDateTime overdueDueDate = LocalDateTime.now().minusDays(7);
        LoanRecord loan = loanRecordRepository.save(
            new LoanRecord(copy, member, borrowedAt, overdueDueDate));

        LocalDateTime before = LocalDateTime.now();
        circulationService.extendLoan(loan.getId(), new ExtendLoanRequest(3));

        LoanRecord updated = loanRecordRepository.findById(loan.getId()).orElseThrow();
        assertThat(updated.getDueDate()).isAfter(before.plusDays(2));
        assertThat(updated.getDueDate()).isBefore(before.plusDays(4));
    }
}
