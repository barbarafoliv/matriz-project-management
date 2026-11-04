package com.matriz.projects;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

interface ProjectMemberRepository extends JpaRepository<ProjectMember, ProjectMember.Key> {

	Optional<ProjectMember> findByProjectIdAndUserId(long projectId, long userId);

	List<ProjectMember> findByProjectId(long projectId);

	List<ProjectMember> findByUserId(long userId);

}
