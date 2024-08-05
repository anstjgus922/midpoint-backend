package com.solucitation.midpoint_backend.domain.member.repository;

import com.solucitation.midpoint_backend.domain.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MemberRepository extends JpaRepository<Member, String> {
    Optional<Member> findByEmail(String email);
    Optional<Member> findByNickname(String nickname);

    Optional<Member> findByLoginId(String loginId);
    Optional<Member> findByEmailOrLoginId(String email, String nickname);
    Optional<Member> findByName(String name);
}
