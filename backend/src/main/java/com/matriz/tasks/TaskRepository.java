package com.matriz.tasks;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

interface TaskRepository extends JpaRepository<Task, Long>, JpaSpecificationExecutor<Task> {

	Optional<Task> findByIdAndProjectId(long id, long projectId);

	/**
	 * Tasks the user is assigned to, in the given projects, by due date with tasks without
	 * one last (US-06).
	 */
	@Query("""
			select t from Task t
			where t.projectId in :projectIds
			  and exists (select 1 from TaskAssignee a where a.taskId = t.id and a.userId = :userId)
			order by case when t.dueDate is null then 1 else 0 end, t.dueDate, t.id""")
	List<Task> findAssignedTo(long userId, Collection<Long> projectIds);

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
