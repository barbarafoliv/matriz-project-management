package com.matriz.projects;

import java.net.URI;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.matriz.accounts.AuthenticatedUser;

/**
 * Projects (US-02, US-12). The caller is always taken from the session.
 */
@RestController
@RequestMapping("/projects")
class ProjectController {

	private final ProjectService projectService;

	ProjectController(ProjectService projectService) {
		this.projectService = projectService;
	}

	@GetMapping
	List<ProjectView> listProjects(@AuthenticationPrincipal AuthenticatedUser caller,
			@RequestParam(name = "q", required = false) String search) {
		return this.projectService.listProjects(caller.id(), search);
	}

	@PostMapping
	ResponseEntity<ProjectView> createProject(@AuthenticationPrincipal AuthenticatedUser caller,
			@Valid @RequestBody ProjectRequest body) {
		ProjectView project = this.projectService.createProject(caller.id(), body.name(), body.description());
		URI location = ServletUriComponentsBuilder.fromCurrentRequest()
			.path("/{projectId}")
			.buildAndExpand(project.id())
			.toUri();
		return ResponseEntity.created(location).body(project);
	}

	@GetMapping("/{projectId}")
	ProjectView getProject(@AuthenticationPrincipal AuthenticatedUser caller,
			@PathVariable @Positive long projectId) {
		return this.projectService.getProject(caller.id(), projectId);
	}

	@PutMapping("/{projectId}")
	ProjectView updateProject(@AuthenticationPrincipal AuthenticatedUser caller,
			@PathVariable @Positive long projectId, @Valid @RequestBody ProjectRequest body) {
		return this.projectService.updateProject(caller.id(), projectId, body.name(), body.description());
	}

	@DeleteMapping("/{projectId}")
	ResponseEntity<Void> deleteProject(@AuthenticationPrincipal AuthenticatedUser caller,
			@PathVariable @Positive long projectId) {
		this.projectService.deleteProject(caller.id(), projectId);
		return ResponseEntity.noContent().build();
	}

}
