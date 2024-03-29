package study.querydsl.repository;

import jakarta.persistence.EntityManager;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.dto.MemberSearchCondition;
import study.querydsl.dto.MemberTeamDto;
import study.querydsl.entity.Member;
import study.querydsl.entity.Team;


import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class MemberJpaRepositoryTest {
    @Autowired
    EntityManager em;
    @Autowired
    MemberJpaRepository memberJpaRepository;

    @Test
    public void basicTest() throws Exception {
        //given
        Member member1 = new Member("member1", 10);
        memberJpaRepository.save(member1);

        //when
        Member find = memberJpaRepository.findById(member1.getId()).get();
        List<Member> all = memberJpaRepository.findAll();
        List<Member> byName = memberJpaRepository.findByUsername("member1");

        //then
        assertThat(member1).isEqualTo(find);
        assertThat(all).containsExactly(member1);
        assertThat(byName).containsExactly(member1);
    }
    @Test
    public void queryTest() throws Exception {
        //given
        Member member1 = new Member("member1", 10);
        memberJpaRepository.save(member1);

        //when
        Member find = memberJpaRepository.findById(member1.getId()).get();
        List<Member> all = memberJpaRepository.findAll_query();
        List<Member> byName = memberJpaRepository.findByUsername_query("member1");

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
//        List<MemberTeamDto> byCondQuery = memberJpaRepository.findByCond_query(condition);
        List<MemberTeamDto> seach = memberJpaRepository.seach(condition);

        //then
//        assertThat(byCondQuery).extracting("username").containsExactly("member3","member4");
        assertThat(seach).extracting("username").containsExactly("member3","member4");
    }

}