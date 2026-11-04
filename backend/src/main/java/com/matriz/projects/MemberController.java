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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.matriz.accounts.AuthenticatedUser;

/**
 * Project members (US-03).
 */
@RestController
@RequestMapping("/projects/{projectId}/members")
class MemberController {

	private final ProjectService projectService;

	MemberController(ProjectService projectService) {
		this.projectService = projectService;
	}

	@GetMapping
	List<MemberView> listMembers(@AuthenticationPrincipal AuthenticatedUser caller,
			@PathVariable @Positive long projectId) {
		return this.projectService.listMembers(caller.id(), projectId);
	}

	@PostMapping
	ResponseEntity<MemberView> addMember(@AuthenticationPrincipal AuthenticatedUser caller,
			@PathVariable @Positive long projectId, @Valid @RequestBody AddMemberRequest body) {
		MemberView member = this.projectService.addMember(caller.id(), projectId, body.email());
		URI location = ServletUriComponentsBuilder.fromCurrentRequest()
			.path("/{userId}")
			.buildAndExpand(member.userId())
			.toUri();
		return ResponseEntity.created(location).body(member);
	}

	@DeleteMapping("/{userId}")
	ResponseEntity<Void> removeMember(@AuthenticationPrincipal AuthenticatedUser caller,
			@PathVariable @Positive long projectId, @PathVariable @Positive long userId) {
		this.projectService.removeMember(caller.id(), projectId, userId);
		return ResponseEntity.noContent().build();
	}

}
