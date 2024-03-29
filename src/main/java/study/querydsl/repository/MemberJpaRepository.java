package study.querydsl.repository;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;
import study.querydsl.dto.MemberSearchCondition;
import study.querydsl.dto.MemberTeamDto;
import study.querydsl.dto.QMemberDto;
import study.querydsl.dto.QMemberTeamDto;
import study.querydsl.entity.Member;
import study.querydsl.entity.QMember;
import study.querydsl.entity.QTeam;

import java.util.List;
import java.util.Optional;

import static study.querydsl.entity.QMember.*;
import static study.querydsl.entity.QTeam.*;

@Repository
public class MemberJpaRepository {
    private final EntityManager em;

    private final JPAQueryFactory queryFactory;

    public MemberJpaRepository(EntityManager em) {
        this.em = em;
        this.queryFactory = new JPAQueryFactory(em);
    }

    public void save(Member member) {
        em.persist(member);
    }

    public Optional<Member> findById(Long id) {
        Member member = em.find(Member.class, id);
        return Optional.ofNullable(member);
    }

    public List<Member> findAll() {
        return em.createQuery("select m from Member m", Member.class).getResultList();
    }

    public List<Member> findByUsername(String name) {
        return em.createQuery("select m from Member m where username=:name", Member.class).setParameter("name",name).getResultList();
    }
    public List<Member> findAll_query() {
        return queryFactory.
                selectFrom(member).fetch();
    }

    public List<Member> findByUsername_query(String name) {
        return queryFactory.
                selectFrom(member).where(member.username.eq(name)).fetch();
    }

    public List<MemberTeamDto> findByCond_query(MemberSearchCondition cond) {
        BooleanBuilder booleanBuilder = new BooleanBuilder();
        if(StringUtils.hasText(cond.getUsername())){
            booleanBuilder.and(member.username.eq(cond.getUsername()));
        }
        if(StringUtils.hasText(cond.getTeamName())){
            booleanBuilder.and(team.name.eq(cond.getTeamName()));
        }
        if (cond.getAgeGoe() != null) {
            booleanBuilder.and(member.age.goe(cond.getAgeGoe()));
        }
        if (cond.getAgeLoe() != null) {
            booleanBuilder.and(member.age.loe(cond.getAgeLoe()));
        }
        return queryFactory.
                select(new QMemberTeamDto(
                        member.id.as("memberId"),
                        member.username,
                        member.age,
                        team.id.as("teamId"),
                        team.name.as("teamName")))
                .from(member)
                .leftJoin(member.team, team)
                .where(booleanBuilder)
                .fetch();
    }

    public List<MemberTeamDto> seach(MemberSearchCondition cond) {

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
        if(StringUtils.hasText(teamName)){
            return team.name.eq(teamName);
        }

        return null;
    }

    private BooleanExpression usernameEq(String username) {
        if(StringUtils.hasText(username)){
            return  member.username.eq(username);
        }
        return null;
    }


}
