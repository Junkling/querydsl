package study.querydsl;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.QueryResults;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Commit;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import study.querydsl.dto.MemberDto;
import study.querydsl.dto.QMemberDto;
import study.querydsl.dto.UserDto;
import study.querydsl.entity.Member;
import study.querydsl.entity.QMember;
import study.querydsl.entity.Team;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static study.querydsl.entity.QMember.member;
import static study.querydsl.entity.QTeam.team;

@SpringBootTest
@Transactional
public class QuerydslBasicTest {
    @Autowired
    EntityManager em;
    JPAQueryFactory queryFactory;

    @BeforeEach
    public void before() {
        queryFactory = new JPAQueryFactory(em);

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
    }

    @Test
    public void jpql() {
        Member findByJPQL = em.createQuery("select m from Member m where m.username = :username", Member.class).setParameter("username", "member1").getSingleResult();
        assertThat(findByJPQL.getUsername()).isEqualTo("member1");
    }

    @Test
    public void querydsl() {
//        QMember m = new QMember("m");
//        Member findByDsl = queryFactory.select(m).from(m).where(m.username.eq("member1")).fetchOne();
        Member findByDsl = queryFactory.select(member).from(member).where(member.username.eq("member1")).fetchOne();

        assertThat(findByDsl.getUsername()).isEqualTo("member1");
    }

    @Test
    public void select() {
//        QMember m = new QMember("m");
//        Member findByDsl = queryFactory.select(m).from(m).where(m.username.eq("member1")).fetchOne();
        Member findByDsl = queryFactory.selectFrom(member).where(member.username.eq("member1").and(member.age.eq(10))).fetchOne();

        // , 를 쓰면 자동 And 조건 && null 일 경우 무시함
        Member findByDsl2 = queryFactory.selectFrom(member).where(member.username.eq("member1"), member.age.eq(10)).fetchOne();

        assertThat(findByDsl2.getUsername()).isEqualTo("member1");
    }

    @Test
    public void resultFetch() {

        //리스트 조회
        List<Member> fetch = queryFactory.selectFrom(member).fetch();
        // 단건 조회
//        Member member1 = queryFactory.selectFrom(member).fetchOne();

        // Deprecated 됨 , 페이지 네이션 객체가 나옴
        QueryResults<Member> results = queryFactory.selectFrom(member).fetchResults();
        results.getTotal();
        // 리스트 반환
        List<Member> content = results.getResults();
        results.getLimit();
        results.getOffset();

        // Deprecated 됨 , 카운트 쿼리 나감
        long total = queryFactory.selectFrom(member).fetchCount();

        // 성능상 쿼리 두방이 나음
    }

    /**
     * 1. 나이 내림차순
     * 2. 이름 오림차순
     * 3. 회원이름이 없으면 마지막 (null)
     */
    @Test
    public void sort() {
        em.persist(new Member(null, 100));
        em.persist(new Member("member5", 100));
        em.persist(new Member("member6", 100));
        List<Member> fetch = queryFactory
                .selectFrom(member)
                .where(member.age.eq(100))
                .orderBy(member.age.desc(), member.username.asc().nullsLast())
                .fetch();
        assertThat(fetch.get(0).getUsername()).isEqualTo("member5");
        assertThat(fetch.get(1).getUsername()).isEqualTo("member6");
        assertThat(fetch.get(2).getUsername()).isNull();
    }

    @Test
    public void pageing1() {
        List<Member> fetch = queryFactory
                .selectFrom(member)
                .orderBy(member.username.asc().nullsLast())
                .offset(1)
                .limit(2)
                .fetch();
        assertThat(fetch.size()).isEqualTo(2);
    }


    @Test
    public void pageing2() {
        QueryResults<Member> fetch = queryFactory
                .selectFrom(member)
                .orderBy(member.username.asc().nullsLast())
                .offset(1)
                .limit(2)
                .fetchResults();
        assertThat(fetch.getTotal()).isEqualTo(4);
        assertThat(fetch.getLimit()).isEqualTo(2);
        assertThat(fetch.getOffset()).isEqualTo(1);
        assertThat(fetch.getResults().size()).isEqualTo(2);
    }

    @Test
    public void aggregation() {
        List<Tuple> result = queryFactory
                .select(member.count()
                        , member.age.sum()
                        , member.age.avg()
                        , member.age.max()
                        , member.age.min())
                .from(member)
                .fetch();
        Tuple tuple = result.get(0);
        assertThat(tuple.get(member.count())).isEqualTo(4);
        assertThat(tuple.get(member.age.sum())).isEqualTo(100);
        assertThat(tuple.get(member.age.avg())).isEqualTo(25);
        assertThat(tuple.get(member.age.max())).isEqualTo(40);
        assertThat(tuple.get(member.age.min())).isEqualTo(10);
    }

    /**
     * 팀의 이름과 각 팀의 평균 연령
     * having 절도 넣을 수 있다.
     */
    @Test
    public void groupBy() throws Exception {
        //when

        List<Tuple> result = queryFactory
                .select(team.name, member.age.avg())
                .from(member)
                .join(member.team, team)
                .groupBy(team.name)
                .fetch();
        Tuple teamA = result.get(0);
        Tuple teamB = result.get(1);

        //then
        assertThat(teamA.get(team.name)).isEqualTo("teamA");
        assertThat(teamA.get(member.age.avg())).isEqualTo(15);

        assertThat(teamB.get(team.name)).isEqualTo("teamB");
        assertThat(teamB.get(member.age.avg())).isEqualTo(35);
    }

    @Test
    public void join() throws Exception {
        //when
        List<Member> result = queryFactory
                .selectFrom(member)
                .join(member.team, team)
                .where(team.name.eq("teamA"))
                .fetch();

        //then
        assertThat(result).extracting("username").containsExactly("member1", "member2");
    }

    @Test
    public void leftJoin() throws Exception {
        //when
        List<Member> result = queryFactory
                .selectFrom(member)
                .leftJoin(member.team, team)
                .where(team.name.eq("teamA"))
                .fetch();

        //then
    }


    /**
     * 세타 조인 (연관관게 없는 테이블의 막 조인)
     * 회원의 이름이 팀 이름과 같은 회원 조회
     */
    @Test
    public void theta_join() throws Exception {
        //given
        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));
        //when
        List<Member> result = queryFactory
                .select(member)
                .from(member, team)
                .where(member.username.eq(team.name))
                .fetch();

        //then
        assertThat(result).extracting("username").containsExactly("teamA", "teamB");
        assertThat(result.get(0).getUsername()).isEqualTo("teamA");
        assertThat(result.get(1).getUsername()).isEqualTo("teamB");
    }

    /**
     * 회원에 팀을 조인하면서, 팀 이름이 teamA인 팀만 조인 회원은 모두 조회
     * jpal : select m from Member m left join m.team t on t.name = :teamName
     */
    @Test
    public void join_on_filtering() throws Exception {
        String param = "teamA";

        //when
        List<Tuple> result = queryFactory
                .select(member, team)
                .from(member)
//                .join(member.team, team)      // inner 조인
                .leftJoin(member.team, team)    // left outter 조인
//                .on(team.name.eq(param))      // inner 조인의 경우 on 과 where 은 동일한 결과 (left + where 도 같음) 하지만 outter 조인의 경우는 on으로 늘려줘야함
                .where(team.name.eq(param))
                .fetch();
        //then
        for (Tuple tuple : result) {
            System.out.println(tuple);
        }
        /** Tuple 객체
         * [Member(id=1, username=member1, age=10), Team(id=1, name=teamA)]
         * [Member(id=2, username=member2, age=20), Team(id=1, name=teamA)]
         * [Member(id=3, username=member3, age=30), null]
         * [Member(id=4, username=member4, age=40), null]
         */
    }

    /**
     * 연관관계 없는 엔티티 외부 조인
     * 회원 이름과 팀이름이 같은 대상 조인
     */
    @Test
    public void join_notRelation() throws Exception {

        //given
        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));
        em.persist(new Member("teamC"));


        //when
        List<Tuple> result = queryFactory
                .select(member, team)
                .from(member)
                .leftJoin(team) //조인하는 테이블의 연관 엔티티가 들어가는것이 아닌 관계 없는 엔티티 Q타입이 들어가고 on 절에서 조건 정의
                .on(member.username.eq(team.name))
                .fetch();


        //then

        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }
    }

    @PersistenceUnit
    EntityManagerFactory emf;

    @Test
    public void without_fetchJoin() throws Exception {
        //given
        em.flush();
        em.clear();
        //when
        Member findMember = queryFactory
                .selectFrom(member)
                .where(member.username.eq("member1"))
                .fetchOne();
        //then
        assertThat(emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam())).as("without_fetch_join").isFalse();
    }

    @Test
    public void with_fetchJoin() throws Exception {
        //given
        em.flush();
        em.clear();
        //when
        Member findMember = queryFactory
                .selectFrom(member)
                .join(member.team, team)
                .fetchJoin()
                .where(member.username.eq("member1"))
                .fetchOne();
        //then
//        assertThat(emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam())).as("without_fetch_join").isFalse();
        assertThat(emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam())).as("without_fetch_join").isTrue();
    }

    /**
     * 나이가 가장 많은 회원
     */
    @Test
    public void subQuery() throws Exception {
        QMember sub = new QMember("memberSub");
        //when
        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.eq(
                        JPAExpressions
                                .select(sub.age.max())
                                .from(sub)))    //fetch는 하지 않는다
                .fetch();

        //then
        assertThat(result).extracting("age").containsExactly(40);
    }

    /**
     * 나이가 평균 이상 회원
     */
    @Test
    public void subQueryGoe() throws Exception {
        QMember sub = new QMember("memberSub");
        //when
        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.goe(
                        JPAExpressions
                                .select(sub.age.avg())
                                .from(sub)))    //fetch는 하지 않는다
                .fetch();

        //then
        assertThat(result).extracting("age").containsExactly(30, 40);
    }

    /**
     * 나이가 평균 이상 회원
     */
    @Test
    public void subQueryIn() throws Exception {
        QMember sub = new QMember("memberSub");
        //when
        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.in(
                        JPAExpressions
                                .select(sub.age)
                                .from(sub)
                                .where(sub.age.gt(10))))    //fetch는 하지 않는다
                .fetch();

        //then
        assertThat(result).extracting("age").containsExactly(20, 30, 40);
    }

    @Test
    public void selectSubQuery() throws Exception {
        QMember sub = new QMember("memberSub");
        //when

        List<Tuple> result = queryFactory
                .select(member.username,
                        JPAExpressions
                                .select(sub.age.avg())
                                .from(sub))
                .from(member)
                .fetch();
        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }
    }
    /*
    !! JPA 의 한계 : from 절 서브쿼리가 되지 않는다. 해결방안은 아래와 같다.!!
    1. from 절의 서브쿼리를 join 으로 대체 (불가능한 상황이 간혹 있다)
    2. 애플리케이션에서 쿼리를 2번으로 분리하여 실행
    3. nativeSQL 을 사용한다.

    중요!! : 쿼리는 데이터를 퍼올리는 용도로만 사용하고 어플리케이션단에서 조합한다.
    쓸대 없는 서브쿼리의 남용을 피해야하며 한방 쿼리가 필요한 시점이 언제인지 고민해야한다.
    (필요 시점) ->> 전체 조회와 같은 성능이 중요한 경우에 한방
    */

    @Test
    public void basicCase() throws Exception {
        //when
        List<String> result = queryFactory
                .select(member.age
                        .when(10).then("열살")
                        .when(20).then("스무")
                        .otherwise("기타")
                )
                .from(member)
                .fetch();

        //then
        for (String s : result) {
            System.out.println(s);
        }
    }


    //쿼리에서 해야하는지 고민이 필요 (어플리케이션에서 처리하는게 나을 가능성이 높음)
    @Test
    public void complexCase() throws Exception {
        //when
        List<String> result = queryFactory
                .select(new CaseBuilder()
                        .when(member.age.between(0, 7)).then("미취학 아동")
                        .when(member.age.between(7, 13)).then("초등학생")
                        .when(member.age.between(14, 18)).then("청소년")
                        .when(member.age.between(18, 28)).then("대학생")
                        .otherwise("성인")
                )
                .from(member)
                .fetch();

        //then
        for (String s : result) {
            System.out.println(s);
        }
    }

    @Test
    public void constant() throws Exception {
        //when
        List<Tuple> result = queryFactory
                .select(member.username, Expressions.constant("A"))
                .from(member)
                .fetch();

        //then
        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }
    }

    @Test
    public void concat() throws Exception {
        //when
        List<String> result = queryFactory
                .select(member.username.concat("_ 나이 : ").concat(member.age.stringValue()))
                .from(member)
                .fetch();

        //then
        for (String tuple : result) {
            System.out.println("tuple = " + tuple);
        }
    }

    @Test
    public void simpleProjection() throws Exception {
        //when
        // 반환값이 단일 객체 타입
        List<String> result = queryFactory.select(member.username)
                .from(member)
                .fetch();
        List<Member> memberResult = queryFactory.select(member)
                .from(member)
                .fetch();

        //then
        for (String s : result) {
            System.out.println("s = " + s);
        }
        for (Member member1 : memberResult) {
            System.out.println("member = " + member1);
        }
    }

    //Tuple이 repository 보다 앞단에서 사용되는거 보단 repository에서 작업이 되는걸 권장
    @Test
    public void tupleProjection() throws Exception {
        //when
        List<Tuple> result = queryFactory.select(member.username, member.age)
                .from(member)
                .fetch();
        //then
        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
            String username = tuple.get(member.username);
            Integer age = tuple.get(member.age);
            System.out.println("tuple.get(member.username) = " + username);
            System.out.println("tuple.get(member.age) = " + age);
        }
    }

    @Test
    public void findDtoByJPQL() throws Exception {
        //given

        //생성자로 DTO 변환을 해야함
        List<MemberDto> resultList = em.createQuery("select new study.querydsl.dto.MemberDto(m.username, m.age) m from Member m", MemberDto.class).getResultList();

        for (MemberDto memberDto : resultList) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    //세터로 값 할당
    @Test
    public void findDtoBySetter() throws Exception {
        //given
        List<MemberDto> result = queryFactory
                .select(Projections.bean(MemberDto.class,
                        member.username,
                        member.age))
                .from(member)
                .fetch();

        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    //필드에 직접 값 할당
    @Test
    public void findDtoByField() throws Exception {
        //given
        List<MemberDto> result = queryFactory
                .select(Projections.fields(MemberDto.class,
                        member.username,
                        member.age))
                .from(member)
                .fetch();

        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }
    //필드와 세터는 엔티티와 Dto 의 필드가 매칭되어야 값이 할당됨, as로 Dto의 필드를 지정할 수 있음

    @Test
    public void findDtoByConstructor() throws Exception {
        //given
        List<MemberDto> result = queryFactory
                .select(Projections.constructor(MemberDto.class,
                        member.username,
                        member.age))
                .from(member)
                .fetch();

        List<UserDto> UserResult = queryFactory
                .select(Projections.constructor(UserDto.class,
                        member.username,
                        member.age))
                .from(member)
                .fetch();

        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
        for (UserDto userDto : UserResult) {
            System.out.println("userDto = " + userDto);
        }
    }

    @Test
    public void findUserDto() throws Exception {
        //given
        QMember sub = new QMember("sub");

        List<UserDto> userResult = queryFactory
                .select(Projections.fields(UserDto.class,
                        member.username.as("name"),
                        member.age))
                .from(member)
                .fetch();

        List<UserDto> userResult2 = queryFactory
                .select(Projections.fields(UserDto.class,
                        member.username.as("name"),

                        ExpressionUtils.as(JPAExpressions
                                .select(sub.age.max())
                                .from(sub), "age")))
                .from(member)
                .fetch();


        //when
        for (UserDto userDto : userResult) {
            System.out.println("userDto = " + userDto);
        }
        for (UserDto userDto : userResult2) {
            System.out.println("userDto2 = " + userDto);
        }
        //then
    }

    @Test
    public void findDtoByQueryProjection() throws Exception {
        //given
        List<MemberDto> result = queryFactory
                .select(new QMemberDto(member.username, member.age))
                .from(member)
                .fetch();

        //when

        //then
        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    @Test
    public void dynamicQuery_booleanBuilder() throws Exception {
        String usernameParam = "member1";
        Integer ageParam = null;
        //when
        List<Member> result = selectMember_1(usernameParam, ageParam);

        for (Member member1 : result) {
            System.out.println("member1 = " + member1);
        }
        //then
    }

    private List<Member> selectMember_1(String usernameParam, Integer ageParam) {
        BooleanBuilder booleanBuilder = new BooleanBuilder();

        if (StringUtils.hasText(usernameParam)) {
            booleanBuilder.and(member.username.eq(usernameParam));
        }
        if (ageParam != null) {
            booleanBuilder.and(member.age.eq(ageParam));
        }
        return queryFactory.
                selectFrom(member)
                .where(booleanBuilder)
                .fetch();
    }

    @Test
    public void dynamicQuery_whereParam() throws Exception {
        String usernameParam = null;
        Integer ageParam = 10;
        //when
        List<Member> result = selectMember_2(usernameParam, ageParam);

        for (Member member1 : result) {
            System.out.println("member1 and 20살 = " + member1);
        }
        //then
    }

    private List<Member> selectMember_2(String usernameParam, Integer ageParam) {

        return queryFactory
                .selectFrom(member)
//                .where(usernameEq(usernameParam), ageEq(ageParam))
                .where(allEq(usernameParam, ageParam))
                .fetch();
    }

    private BooleanExpression ageEq(Integer ageParam) {
        return ageParam != null ? member.age.eq(ageParam) : null;
    }

    private BooleanExpression usernameEq(String usernameParam) {
        return StringUtils.hasText(usernameParam) ? member.username.eq(usernameParam) : null;
    }

    private BooleanExpression allEq(String usernameParam, Integer ageParam) {
        if (!StringUtils.hasText(usernameParam) && ageParam == null) {
            return null;
        } else if (ageParam != null && !StringUtils.hasText(usernameParam)) {
            return ageEq(ageParam);
        } else if (StringUtils.hasText(usernameParam) && ageParam == null) {
            return usernameEq(usernameParam);
        }
        return usernameEq(usernameParam).and(ageEq(ageParam));
    }

    @Test
    @Commit
    public void bulkUpdate() throws Exception {
        //given
        long count = queryFactory
                .update(member)
                .set(member.username, "늙은이")
                .where(member.age.goe(30))
                .execute();
//        System.out.println("count = " + count);
        em.flush();
        em.clear();

        List<Member> fetch = queryFactory.selectFrom(member).fetch();
        for (Member fetch1 : fetch) {
            System.out.println("fetch1 = " + fetch1);
        }
        //when

        //then
    }

    @Test
    public void bulkAdd() throws Exception {
        //given
        long execute = queryFactory
                .update(member)
//                .set(member.age, member.age.add(1))
                .set(member.age, member.age.multiply(2))
                .execute();

        //when

        //then
    }

    @Test
    public void bulkDelete() throws Exception {
        //given
        long execute = queryFactory
                .delete(member)
//                .set(member.age, member.age.add(1))
                .where(member.age.goe(30))
                .execute();

        //when

        //then
    }

    @Test
    public void sqlFunction() throws Exception {
        //given
        List<String> result = queryFactory
                .select(
                        Expressions.stringTemplate(
                                "function('replace', {0},{1},{2})",
                                member.username, "member", "M"))
                .from(member)
                .fetch();
        //when
        for (String s : result) {
            System.out.println("s = " + s);
        }
        //then
    }

    @Test
    public void sqlFunction2() throws Exception {
        //given
        List<String> result = queryFactory
                .select(member.username)
                .from(member)
                .where(member.username.eq(
//                        Expressions.stringTemplate("function('lower', {0})", member.username))
                        member.username.lower())
                )
                .fetch();
        //when
        for (String s : result) {
            System.out.println("s = " + s);
        }
        //then
    }
}
