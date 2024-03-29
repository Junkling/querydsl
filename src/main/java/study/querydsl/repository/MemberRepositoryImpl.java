package study.querydsl.repository;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.QueryResults;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.JPQLQuery;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.support.QuerydslRepositorySupport;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.util.StringUtils;
import study.querydsl.dto.MemberSearchCondition;
import study.querydsl.dto.MemberTeamDto;
import study.querydsl.dto.QMemberTeamDto;
import study.querydsl.entity.Member;

import java.util.List;

import static study.querydsl.entity.QMember.member;
import static study.querydsl.entity.QTeam.team;

public class MemberRepositoryImpl extends QuerydslRepositorySupport implements MemberRepositoryCustom {
    private final JPAQueryFactory queryFactory;
//    public MemberRepositoryImpl(EntityManager em) {
//    }


    public MemberRepositoryImpl(EntityManager em) {
        super(Member.class);
        this.queryFactory = new JPAQueryFactory(em);
    }

    public List<MemberTeamDto> findByCond_query(MemberSearchCondition cond) {
        BooleanBuilder booleanBuilder = new BooleanBuilder();
        if (StringUtils.hasText(cond.getUsername())) {
            booleanBuilder.and(member.username.eq(cond.getUsername()));
        }
        if (StringUtils.hasText(cond.getTeamName())) {
            booleanBuilder.and(team.name.eq(cond.getTeamName()));
        }
        if (cond.getAgeGoe() != null) {
            booleanBuilder.and(member.age.goe(cond.getAgeGoe()));
        }
        if (cond.getAgeLoe() != null) {
            booleanBuilder.and(member.age.loe(cond.getAgeLoe()));
        }

        return from(member)
                .leftJoin(member.team, team)
                .where(booleanBuilder)
                .select(new QMemberTeamDto(
                        member.id.as("memberId"),
                        member.username,
                        member.age,
                        team.id.as("teamId"),
                        team.name.as("teamName")))
                .fetch();

//        return queryFactory.
//                select(new QMemberTeamDto(
//                        member.id.as("memberId"),
//                        member.username,
//                        member.age,
//                        team.id.as("teamId"),
//                        team.name.as("teamName")))
//                .from(member)
//                .leftJoin(member.team, team)
//                .where(booleanBuilder)
//                .fetch();
    }

    @Override
    public List<MemberTeamDto> search(MemberSearchCondition cond) {

        return queryFactory.
                select(new QMemberTeamDto(
                        member.id.as("memberId"),
                        member.username,
                        member.age,
                        team.id.as("teamId"),
                        team.name.as("teamName")))
                .from(member)
                .leftJoin(member.team, team)
                .where(usernameEq(cond.getUsername()), teamNameEq(cond.getTeamName()), ageGoe(cond.getAgeGoe()), ageLoe(cond.getAgeLoe()))
                .fetch();
    }

    @Override
    public Page<MemberTeamDto> searchPageSimple(MemberSearchCondition cond, Pageable pageable) {
        QueryResults<MemberTeamDto> result = queryFactory.
                select(new QMemberTeamDto(
                        member.id.as("memberId"),
                        member.username,
                        member.age,
                        team.id.as("teamId"),
                        team.name.as("teamName")))
                .from(member)
                .leftJoin(member.team, team)
                .where(usernameEq(cond.getUsername()), teamNameEq(cond.getTeamName()), ageGoe(cond.getAgeGoe()), ageLoe(cond.getAgeLoe()))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetchResults();
        List<MemberTeamDto> content = result.getResults();
        long total = result.getTotal();
        return new PageImpl<>(content, pageable, total);
    }
    //sort 되지 않음 from 으로 시작되는 구조..
    public Page<MemberTeamDto> searchPageSimple2(MemberSearchCondition cond, Pageable pageable) {
        JPQLQuery<MemberTeamDto> query = from(member)
                .leftJoin(member.team, team)
                .where(usernameEq(cond.getUsername()), teamNameEq(cond.getTeamName()), ageGoe(cond.getAgeGoe()), ageLoe(cond.getAgeLoe()))
                .select(new QMemberTeamDto(
                        member.id.as("memberId"),
                        member.username,
                        member.age,
                        team.id.as("teamId"),
                        team.name.as("teamName")));
        JPQLQuery<MemberTeamDto> pagingQuery = getQuerydsl().applyPagination(pageable, query);
        return new PageImpl<>( pagingQuery.fetch(), pageable, pagingQuery.fetchCount());
    }

    @Override
    public Page<MemberTeamDto> searchPageComplex(MemberSearchCondition cond, Pageable pageable) {
        List<MemberTeamDto> result = queryFactory.
                select(new QMemberTeamDto(
                        member.id.as("memberId"),
                        member.username,
                        member.age,
                        team.id.as("teamId"),
                        team.name.as("teamName")))
                .from(member)
                .leftJoin(member.team, team)
                .where(usernameEq(cond.getUsername()), teamNameEq(cond.getTeamName()), ageGoe(cond.getAgeGoe()), ageLoe(cond.getAgeLoe()))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();
//        Long total = queryFactory
//                .select(member.count())
//                .from(member)
//                .leftJoin(member.team, team)
//                .where(usernameEq(cond.getUsername()), teamNameEq(cond.getTeamName()), ageGoe(cond.getAgeGoe()), ageLoe(cond.getAgeLoe()))
//                .fetchFirst();

        JPAQuery<Member> count = queryFactory
                .selectFrom(member)
                .leftJoin(member.team, team)
                .where(usernameEq(cond.getUsername()), teamNameEq(cond.getTeamName()), ageGoe(cond.getAgeGoe()), ageLoe(cond.getAgeLoe()));
        return PageableExecutionUtils.getPage(result, pageable, count::fetchCount);
//        return new PageImpl<>(result, pageable, total);
    }

    private BooleanExpression ageLoe(Integer ageLoe) {

        if (ageLoe != null) {
            member.age.loe(ageLoe);
        }
        return null;
    }

    private BooleanExpression ageGoe(Integer ageGoe) {
        if (ageGoe != null) {
            member.age.goe(ageGoe);
        }
        return null;
    }

    private BooleanExpression teamNameEq(String teamName) {
        if (StringUtils.hasText(teamName)) {
            return team.name.eq(teamName);
        }

        return null;
    }

    private BooleanExpression usernameEq(String username) {
        if (StringUtils.hasText(username)) {
            return member.username.eq(username);
        }
        return null;
    }

}
