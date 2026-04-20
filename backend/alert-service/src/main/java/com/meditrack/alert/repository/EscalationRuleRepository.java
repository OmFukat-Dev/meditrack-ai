package com.meditrack.alert.repository;

import com.meditrack.alert.entity.EscalationRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EscalationRuleRepository extends JpaRepository<EscalationRule, String> {
    
    @Query("SELECT r FROM EscalationRule r WHERE r.alertType = :alertType AND r.priority = :priority AND r.active = true")
    List<EscalationRule> findByAlertTypeAndPriorityAndActive(
        @Param("alertType") String alertType, 
        @Param("priority") EscalationRule.AlertPriority priority, 
        @Param("active") Boolean active);
    
    @Query("SELECT r FROM EscalationRule r WHERE r.alertType = :alertType AND r.active = true")
    List<EscalationRule> findByAlertTypeAndActive(@Param("alertType") String alertType, @Param("active") Boolean active);
    
    @Query("SELECT r FROM EscalationRule r WHERE r.priority = :priority AND r.active = true")
    List<EscalationRule> findByPriorityAndActive(@Param("priority") EscalationRule.AlertPriority priority, @Param("active") Boolean active);
    
    @Query("SELECT r FROM EscalationRule r WHERE r.active = true ORDER BY r.escalationLevel ASC")
    List<EscalationRule> findByActiveOrderByEscalationLevelAsc(@Param("active") Boolean active);
    
    @Query("SELECT r FROM EscalationRule r WHERE r.targetRole = :targetRole AND r.active = true")
    List<EscalationRule> findByTargetRoleAndActive(@Param("targetRole") String targetRole, @Param("active") Boolean active);
    
    @Query("SELECT r FROM EscalationRule r WHERE r.escalationLevel = :escalationLevel AND r.active = true")
    List<EscalationRule> findByEscalationLevelAndActive(@Param("escalationLevel") String escalationLevel, @Param("active") Boolean active);
    
    @Query("SELECT r FROM EscalationRule r WHERE r.timeBasedEscalation = true AND r.active = true")
    List<EscalationRule> findByTimeBasedEscalationAndActive(@Param("active") Boolean active);
    
    @Query("SELECT r FROM EscalationRule r WHERE r.conditionBasedEscalation = true AND r.active = true")
    List<EscalationRule> findByConditionBasedEscalationAndActive(@Param("active") Boolean active);
    
    @Query("SELECT r FROM EscalationRule r WHERE r.priorityBasedEscalation = true AND r.active = true")
    List<EscalationRule> findByPriorityBasedEscalationAndActive(@Param("active") Boolean active);
}
