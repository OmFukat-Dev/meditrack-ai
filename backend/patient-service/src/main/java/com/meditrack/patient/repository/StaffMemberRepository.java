package com.meditrack.patient.repository;

import com.meditrack.patient.entity.StaffMember;
import com.meditrack.patient.entity.StaffMember.StaffRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StaffMemberRepository extends JpaRepository<StaffMember, Long> {

    Optional<StaffMember> findByEmailIgnoreCase(String email);

    List<StaffMember> findByRoleOrderByFullNameAsc(StaffRole role);

    List<StaffMember> findByRoleAndActiveTrueOrderByFullNameAsc(StaffRole role);

    List<StaffMember> findByActiveTrueOrderByFullNameAsc();
}
