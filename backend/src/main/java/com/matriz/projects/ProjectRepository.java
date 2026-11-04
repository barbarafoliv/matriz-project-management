package com.matriz.projects;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

interface ProjectRepository extends JpaRepository<Project, Long> {

	/**
	 * Projects the user is a member of, sorted by name (EC-04).
	 */
	@Query("""
			select p from Project p, ProjectMember m
			where m.projectId = p.id and m.userId = :userId
			order by lower(p.name), p.id""")
	List<Project> findForMember(long userId);

	/**
	 * Projects the user is a member of whose name matches the pattern, ignoring case (US-12).
	 */
	@Query("""
			select p from Project p, ProjectMember m
			where m.projectId = p.id and m.userId = :userId
			  and lower(p.name) like :pattern escape '!'
			order by lower(p.name), p.id""")
	List<Project> searchForMember(long userId, String pattern);

}
