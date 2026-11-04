package com.matriz.tasks;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

interface CommentRepository extends JpaRepository<Comment, Long> {

	/** Oldest first (US-11). */
	List<Comment> findByTaskIdOrderByCreatedAtAscIdAsc(long taskId);

}
