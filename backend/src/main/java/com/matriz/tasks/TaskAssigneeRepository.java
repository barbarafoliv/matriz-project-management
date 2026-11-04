package com.matriz.tasks;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

interface TaskAssigneeRepository extends JpaRepository<TaskAssignee, TaskAssignee.Key> {

	List<TaskAssignee> findByTaskIdIn(Collection<Long> taskIds);

}
