package com.library.backend.service;

import com.library.backend.dto.LoanDto.*;
import com.library.backend.entity.BookCopy;
import com.library.backend.entity.CopyStatus;
import com.library.backend.entity.LoanRecord;
import com.library.backend.entity.Role;
import com.library.backend.entity.User;
import com.library.backend.exception.BadRequestException;
import com.library.backend.exception.ConflictException;
import com.library.backend.exception.ResourceNotFoundException;
import com.library.backend.repository.BookCopyRepository;
import com.library.backend.repository.LoanRecordRepository;
import com.library.backend.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class CirculationService {

    private final UserRepository userRepository;
    private final BookCopyRepository bookCopyRepository;
    private final LoanRecordRepository loanRecordRepository;

    public CirculationService(
            UserRepository userRepository,
            BookCopyRepository bookCopyRepository,
            LoanRecordRepository loanRecordRepository) {
        this.userRepository = userRepository;
        this.bookCopyRepository = bookCopyRepository;
        this.loanRecordRepository = loanRecordRepository;
    }

    @Transactional
    public LoanActionResponse checkout(CheckoutRequest request) {
        User member = userRepository.findByCustomCode(request.memberCustomCode().trim())
            .orElseThrow(() -> new ResourceNotFoundException("Member with code " + request.memberCustomCode() + " not found"));

        if (member.getRole() != Role.MEMBER || !Boolean.TRUE.equals(member.getActive())) {
            throw new BadRequestException("Member account is inactive or invalid");
        }

        BookCopy copy = bookCopyRepository.findByBarcode(request.barcode().trim())
            .orElseThrow(() -> new ResourceNotFoundException("Copy with barcode " + request.barcode() + " not found"));

        if (copy.getStatus() != CopyStatus.AVAILABLE) {
            throw new ConflictException("Copy is not AVAILABLE (current status: " + copy.getStatus() + ")");
        }

        // Double check no active loan record exists
        if (loanRecordRepository.findByCopyBarcodeAndReturnedAtIsNull(copy.getBarcode()).isPresent()) {
            throw new ConflictException("Copy is already recorded in an active loan");
        }

        LocalDateTime borrowedAt = LocalDateTime.now();
        LocalDateTime dueDate = borrowedAt.plusDays(request.daysToBorrow());

        copy.setStatus(CopyStatus.BORROWED);
        bookCopyRepository.save(copy);

        LoanRecord loan = new LoanRecord(copy, member, borrowedAt, dueDate);
        LoanRecord savedLoan = loanRecordRepository.save(loan);

        return new LoanActionResponse(
            "Book checked out successfully",
            savedLoan.getId(),
            copy.getBarcode(),
            savedLoan.getDueDate(),
            null
        );
    }

    @Transactional
    public LoanActionResponse returnBook(ReturnRequest request) {
        String barcode = request.barcode().trim();
        BookCopy copy = bookCopyRepository.findByBarcode(barcode)
            .orElseThrow(() -> new ResourceNotFoundException("Copy with barcode " + barcode + " not found"));

        LoanRecord loan = loanRecordRepository.findByCopyBarcodeAndReturnedAtIsNull(barcode)
            .orElseThrow(() -> new ResourceNotFoundException("No active loan found for copy barcode " + barcode));

        LocalDateTime returnedAt = LocalDateTime.now();
        loan.setReturnedAt(returnedAt);
        loanRecordRepository.save(loan);

        copy.setStatus(CopyStatus.AVAILABLE);
        bookCopyRepository.save(copy);

        return new LoanActionResponse(
            "Book returned successfully",
            loan.getId(),
            barcode,
            loan.getDueDate(),
            returnedAt
        );
    }

    @Transactional
    public LoanActionResponse extendLoan(Long loanId, ExtendLoanRequest request) {
        LoanRecord loan = loanRecordRepository.findById(loanId)
            .orElseThrow(() -> new ResourceNotFoundException("Loan record with id " + loanId + " not found"));

        if (loan.getReturnedAt() != null) {
            throw new ConflictException("Cannot extend a returned loan");
        }

        LocalDateTime now = LocalDateTime.now();
        // If overdue, extend from today rather than remaining in the past
        LocalDateTime base = loan.getDueDate().isBefore(now) ? now : loan.getDueDate();
        LocalDateTime newDueDate = base.plusDays(request.additionalDays());

        loan.setDueDate(newDueDate);
        loanRecordRepository.save(loan);

        return new LoanActionResponse(
            "Loan extended successfully",
            loan.getId(),
            loan.getCopy().getBarcode(),
            newDueDate,
            null
        );
    }

    @Transactional(readOnly = true)
    public List<ActiveLoanResponse> getActiveLoans() {
        List<LoanRecord> loans = loanRecordRepository.findAllByReturnedAtIsNullOrderByDueDateAsc();
        LocalDateTime now = LocalDateTime.now();

        return loans.stream().map(lr -> new ActiveLoanResponse(
            lr.getId(),
            lr.getCopy().getBook().getTitle(),
            lr.getCopy().getBook().getIsbn(),
            lr.getCopy().getBarcode(),
            lr.getMember().getName(),
            lr.getMember().getCustomCode(),
            lr.getBorrowedAt(),
            lr.getDueDate(),
            lr.getDueDate().isBefore(now)
        )).toList();
    }
}
