package study.querydsl.repository;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;
import study.querydsl.dto.MemberSearchCondition;
import study.querydsl.entity.Member;
import study.querydsl.entity.QMember;
import study.querydsl.repository.support.MyQuerydslRepositorySupport;

import java.util.List;

import static study.querydsl.entity.QMember.member;
import static study.querydsl.entity.QTeam.team;

@Repository
public class MemberTestRepository extends MyQuerydslRepositorySupport {
    public MemberTestRepository(Class<?> domainClass) {
        super(Member.class);
    }

    public List<Member> basicSelect() {
        return select(member)
                .from(member)
                .fetch();
    }

    public List<Member> basicSelectFrom() {
        return selectFrom(member)
                .fetch();
    }

    public Page<Member> searchPage(MemberSearchCondition cond, Pageable pageable) {
        JPAQuery<Member> query = selectFrom(member)
                .leftJoin(member.team, team)
                .where(usernameEq(cond.getUsername()), teamNameEq(cond.getTeamName()), ageGoe(cond.getAgeGoe()), ageLoe(cond.getAgeLoe()));
        List<Member> content = getQuerydsl().applyPagination(pageable, query).fetch();
        return PageableExecutionUtils.getPage(content, pageable, query::fetchCount);
    }

    public Page<Member> applyPagination(MemberSearchCondition cond, Pageable pageable) {
        return applyPagination(pageable, query -> query
                .selectFrom(member)
                .leftJoin(member.team, team)
                .where(usernameEq(cond.getUsername()),
                        teamNameEq(cond.getTeamName()),
                        ageGoe(cond.getAgeGoe()),
                        ageLoe(cond.getAgeLoe()))
        );
    }

    public Page<Member> applyPagination2(MemberSearchCondition cond, Pageable pageable) {
        return applyPagination(pageable,
                query -> query
                        .selectFrom(member)
                        .leftJoin(member.team, team)
                        .where(usernameEq(cond.getUsername()),
                                teamNameEq(cond.getTeamName()),
                                ageGoe(cond.getAgeGoe()),
                                ageLoe(cond.getAgeLoe()))
                , countQuery -> countQuery
                        .select(member.count()).from(member)
                        .leftJoin(member.team, team)
                        .where(usernameEq(cond.getUsername()),
                                teamNameEq(cond.getTeamName()),
                                ageGoe(cond.getAgeGoe()),
                                ageLoe(cond.getAgeLoe())));
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
