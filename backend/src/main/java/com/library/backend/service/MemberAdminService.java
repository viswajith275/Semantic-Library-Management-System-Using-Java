package com.library.backend.service;

import com.library.backend.dto.MemberDto.*;
import com.library.backend.dto.MemberSummaryDto;
import com.library.backend.entity.Role;
import com.library.backend.entity.User;
import com.library.backend.exception.BadRequestException;
import com.library.backend.exception.ConflictException;
import com.library.backend.exception.ResourceNotFoundException;
import com.library.backend.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.List;

@Service
public class MemberAdminService {

    // 8 digits ≈ 10^8 codes. The old 4-digit space (9000) was guessable.
    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserRepository userRepository;

    public MemberAdminService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional
    public MemberResponse createMember(CreateMemberRequest request) {
        String normalizedEmail = User.normalizeEmail(request.email());
        if (normalizedEmail != null && userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new ConflictException("User with email " + normalizedEmail + " already exists");
        }

        String customCode = generateUniqueCustomCode();

        User member = new User();
        member.setName(request.name().trim());
        member.setEmail(normalizedEmail);
        member.setCustomCode(customCode);
        member.setRole(Role.MEMBER);
        member.setActive(true);

        User saved = userRepository.save(member);

        return new MemberResponse(
            saved.getId(),
            saved.getName(),
            saved.getEmail(),
            saved.getCustomCode(),
            saved.getActive()
        );
    }

    @Transactional
    public MemberResponse updateMember(Long id, UpdateMemberRequest request) {
        User member = userRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Member with id " + id + " not found"));

        if (member.getRole() != Role.MEMBER) {
            throw new BadRequestException("Target user is not a member");
        }

        if (request.name() != null && !request.name().isBlank()) {
            member.setName(request.name().trim());
        }

        if (request.email() != null) {
            String normalizedEmail = User.normalizeEmail(request.email());
            if (normalizedEmail != null && !normalizedEmail.equalsIgnoreCase(member.getEmail())) {
                if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
                    throw new ConflictException("User with email " + normalizedEmail + " already exists");
                }
            }
            member.setEmail(normalizedEmail);
        }

        if (request.active() != null) {
            member.setActive(request.active());
        }

        if (Boolean.TRUE.equals(request.regenerateCode())) {
            member.setCustomCode(generateUniqueCustomCode());
        }

        User saved = userRepository.save(member);

        return new MemberResponse(
            saved.getId(),
            saved.getName(),
            saved.getEmail(),
            saved.getCustomCode(),
            saved.getActive()
        );
    }

    @Transactional(readOnly = true)
    public List<MemberSummaryDto> listMembers() {
        return userRepository.findAllMembersWithActiveLoanCount();
    }

    private String generateUniqueCustomCode() {
        String code;
        do {
            code = String.format("MEM-%08d", RANDOM.nextInt(100_000_000));
        } while (userRepository.existsByCustomCode(code));
        return code;
    }
}
