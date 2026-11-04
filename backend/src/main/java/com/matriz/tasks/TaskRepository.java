package com.matriz.tasks;

import java.util.Optional;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

interface TaskRepository extends JpaRepository<Task, Long>, JpaSpecificationExecutor<Task> {

	Optional<Task> findByIdAndProjectId(long id, long projectId);

	static Specification<Task> inProject(long projectId) {
		return (task, query, cb) -> cb.equal(task.get("projectId"), projectId);
	}

	static Specification<Task> withStatus(TaskStatus status) {
		return (task, query, cb) -> (status == null) ? null : cb.equal(task.get("status"), status);
	}

	static Specification<Task> withPriority(Priority priority) {
		return (task, query, cb) -> (priority == null) ? null : cb.equal(task.get("priority"), priority);
	}

}
