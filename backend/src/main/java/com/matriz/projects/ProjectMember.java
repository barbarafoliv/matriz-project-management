package com.matriz.projects;

import java.io.Serializable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

/**
 * Links a user to a project, with their role (FR-03).
 */
@Entity
@Table(name = "project_member")
@IdClass(ProjectMember.Key.class)
class ProjectMember {

	@Id
	@Column(name = "project_id")
	private Long projectId;

	@Id
	@Column(name = "user_id")
	private Long userId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private Role role;

	protected ProjectMember() {
	}

	ProjectMember(long projectId, long userId, Role role) {
		this.projectId = projectId;
		this.userId = userId;
		this.role = role;
	}

	Long getProjectId() {
		return this.projectId;
	}

	Long getUserId() {
		return this.userId;
	}

	Role getRole() {
		return this.role;
	}

	boolean isProjectManager() {
		return this.role == Role.PROJECT_MANAGER;
	}

	record Key(Long projectId, Long userId) implements Serializable {

		Key() {
			this(null, null);
		}

	}

}
