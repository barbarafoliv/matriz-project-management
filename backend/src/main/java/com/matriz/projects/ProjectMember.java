package com.matriz.projects;

import java.io.Serializable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

import org.springframework.data.domain.Persistable;

/**
 * Links a user to a project, with their role (FR-03).
 */
@Entity
@Table(name = "project_member")
@IdClass(ProjectMember.Key.class)
class ProjectMember implements Persistable<ProjectMember.Key> {

	@Id
	@Column(name = "project_id")
	private Long projectId;

	@Id
	@Column(name = "user_id")
	private Long userId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private Role role;

	/**
	 * The key is set before saving, so Spring Data would otherwise merge a new instance: an
	 * existing row with the same key would be updated instead of rejected by the primary key.
	 */
	@Transient
	private boolean isNew = true;

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

	@Override
	public Key getId() {
		return new Key(this.projectId, this.userId);
	}

	@Override
	public boolean isNew() {
		return this.isNew;
	}

	@PostLoad
	@PostPersist
	void markSaved() {
		this.isNew = false;
	}

	record Key(Long projectId, Long userId) implements Serializable {

		Key() {
			this(null, null);
		}

	}

}
