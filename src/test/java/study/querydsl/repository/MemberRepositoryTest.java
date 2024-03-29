package study.querydsl.repository;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.dto.MemberSearchCondition;
import study.querydsl.dto.MemberTeamDto;
import study.querydsl.entity.Member;
import study.querydsl.entity.QMember;
import study.querydsl.entity.Team;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static study.querydsl.entity.QMember.member;

@SpringBootTest
@Transactional
class MemberRepositoryTest {
    @Autowired
    EntityManager em;
    @Autowired
    MemberRepository memberRepository;

    @Test
    public void basicTest() throws Exception {
        //given
        Member member1 = new Member("member1", 10);
        memberRepository.save(member1);

        //when
        Member find = memberRepository.findById(member1.getId()).get();
        List<Member> all = memberRepository.findAll();
        List<Member> byName = memberRepository.findByUsername("member1");

        //then
        assertThat(member1).isEqualTo(find);
        assertThat(all).containsExactly(member1);
        assertThat(byName).containsExactly(member1);
    }

    @Test
    public void searchTest() throws Exception {
        //given
        Team teamA = new Team("teamA");
        Team teamB = new Team("teamB");
        em.persist(teamA);
        em.persist(teamB);

        Member member1 = new Member("member1", 10, teamA);
        Member member2 = new Member("member2", 20, teamA);

        Member member3 = new Member("member3", 30, teamB);
        Member member4 = new Member("member4", 40, teamB);

        em.persist(member1);
        em.persist(member2);
        em.persist(member3);
        em.persist(member4);

        //초기화

        em.flush();
        em.clear();


        //when
        MemberSearchCondition condition = new MemberSearchCondition();
//        condition.setAgeGoe(35);
//        condition.setAgeLoe(40);
        condition.setTeamName("teamB");
//        List<MemberTeamDto> byCondQuery = memberRepository.findByCond_query(condition);
        List<MemberTeamDto> seach = memberRepository.search(condition);

        //then
//        assertThat(byCondQuery).extracting("username").containsExactly("member3","member4");
        assertThat(seach).extracting("username").containsExactly("member3", "member4");
    }

    @Test
    public void searchPageTest() throws Exception {
        //given
        Team teamA = new Team("teamA");
        Team teamB = new Team("teamB");
        em.persist(teamA);
        em.persist(teamB);

        Member member1 = new Member("member1", 10, teamA);
        Member member2 = new Member("member2", 20, teamA);

        Member member3 = new Member("member3", 30, teamB);
        Member member4 = new Member("member4", 40, teamB);

        em.persist(member1);
        em.persist(member2);
        em.persist(member3);
        em.persist(member4);

        //초기화

        em.flush();
        em.clear();


        //when
        MemberSearchCondition condition = new MemberSearchCondition();
//        condition.setAgeGoe(35);
//        condition.setAgeLoe(40);
//        condition.setTeamName("teamB");
//        List<MemberTeamDto> byCondQuery = memberRepository.findByCond_query(condition);
        PageRequest pageRequest = PageRequest.of(0, 3);
        Page<MemberTeamDto> seach = memberRepository.searchPageSimple(condition, pageRequest);

        //then
        assertThat(seach.getSize()).isEqualTo(3);

        assertThat(seach.getContent()).extracting("username").containsExactly("member1", "member2","member3");
    }


    //조인이 불가능한 단점
    //repository 가 querydsl 을 직접적으로 의존해야함
    @Test
    public void querydslPredicateExecutorTest() throws Exception {

        //given
        Team teamA = new Team("teamA");
        Team teamB = new Team("teamB");
        em.persist(teamA);
        em.persist(teamB);

        Member member1 = new Member("member1", 10, teamA);
        Member member2 = new Member("member2", 20, teamA);

        Member member3 = new Member("member3", 30, teamB);
        Member member4 = new Member("member4", 40, teamB);

        em.persist(member1);
        em.persist(member2);
        em.persist(member3);
        em.persist(member4);


        //when
        Iterable<Member> result = memberRepository.findAll(member.age.between(10, 40).and(member.username.eq("member1")));
        for (Member member : result) {
            System.out.println("member1 = " + member);
        }
        //then
    }

}