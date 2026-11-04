package com.matriz.tasks;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * What the Projects module needs from the Tasks module (docs/architecture.md, rules between
 * modules). Only this module reads and writes the task tables.
 */
@Service
public class ProjectTasks {

	private final NamedParameterJdbcTemplate jdbc;

	ProjectTasks(NamedParameterJdbcTemplate jdbc) {
		this.jdbc = jdbc;
	}

	/**
	 * Task counts per status for each project, over all of its tasks (US-14). Projects
	 * without tasks are missing from the result.
	 */
	@Transactional(readOnly = true)
	public Map<Long, TaskCounts> countByStatus(Collection<Long> projectIds) {
		Map<Long, TaskCounts> counts = new HashMap<>();
		if (projectIds.isEmpty()) {
			return counts;
		}
		this.jdbc.query("""
				SELECT project_id,
				       COUNT(*) FILTER (WHERE status = 'TODO') AS todo,
				       COUNT(*) FILTER (WHERE status = 'IN_PROGRESS') AS in_progress,
				       COUNT(*) FILTER (WHERE status = 'DONE') AS done
				FROM task
				WHERE project_id IN (:projectIds)
				GROUP BY project_id""", Map.of("projectIds", projectIds),
				(row) -> {
					counts.put(row.getLong("project_id"),
							new TaskCounts(row.getInt("todo"), row.getInt("in_progress"), row.getInt("done")));
				});
		return counts;
	}

	/**
	 * Removes the user from all tasks of the project, when they stop being a member (US-03).
	 */
	@Transactional
	public void removeAssignee(long projectId, long userId) {
		this.jdbc.update("""
				DELETE FROM task_assignee
				WHERE user_id = :userId
				  AND task_id IN (SELECT id FROM task WHERE project_id = :projectId)""",
				Map.of("projectId", projectId, "userId", userId));
	}

}
