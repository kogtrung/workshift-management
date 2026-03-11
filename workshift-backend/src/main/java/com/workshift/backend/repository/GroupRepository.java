package com.workshift.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.workshift.backend.domain.Group;

public interface GroupRepository extends JpaRepository<Group, Long> {
}
