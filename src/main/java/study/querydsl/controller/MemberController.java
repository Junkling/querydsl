package study.querydsl.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import study.querydsl.dto.MemberSearchCondition;
import study.querydsl.dto.MemberTeamDto;
import study.querydsl.repository.MemberJpaRepository;
import study.querydsl.repository.MemberRepository;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class MemberController {
    private final MemberJpaRepository repository;
    private final MemberRepository memberRepository;

    @GetMapping("/v1")
    public List<MemberTeamDto> searchV1(MemberSearchCondition condition) {
        return repository.findByCond_query(condition);
    }
    @GetMapping("/v2")
    public Page<MemberTeamDto> searchV1(MemberSearchCondition condition, Pageable pageable) {
        return memberRepository.searchPageComplex(condition, pageable);
    }
}
