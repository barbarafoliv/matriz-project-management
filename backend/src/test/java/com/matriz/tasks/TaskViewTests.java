package com.matriz.tasks;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Task views keep their own, unchangeable list of assignees (SpotBugs EI_EXPOSE_REP,
 * docs/quality-security-review.md).
 */
class TaskViewTests {

	@Test
	void theAssigneesCannotBeChangedAfterTheViewIsBuilt() {
		List<UserSummary> assignees = new ArrayList<>(List.of(new UserSummary(1, "Ana Test")));
		TaskView task = new TaskView(1, 2, "Write the copy", null, assignees, null, Priority.MEDIUM, TaskStatus.TODO,
				false, Instant.EPOCH, Instant.EPOCH);
		MyTask myTask = MyTask.of(task, "Team project");

		assignees.add(new UserSummary(2, "Ben Test"));

		assertThat(task.assignees()).extracting(UserSummary::name).containsExactly("Ana Test");
		assertThat(myTask.assignees()).extracting(UserSummary::name).containsExactly("Ana Test");
		assertThatThrownBy(() -> task.assignees().clear()).isInstanceOf(UnsupportedOperationException.class);
		assertThatThrownBy(() -> myTask.assignees().clear()).isInstanceOf(UnsupportedOperationException.class);
	}

}
