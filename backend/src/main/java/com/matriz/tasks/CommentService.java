package com.matriz.tasks;

import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.matriz.accounts.AccountService;
import com.matriz.accounts.User;
import com.matriz.common.web.Text;

/**
 * Comments on tasks (US-11, EC-10). Any project member can add and read them. The author is
 * the signed-in user and the date is the server's: neither comes from the request
 * (Practical Sheet 03, Exercise 3). Comments can't be edited or deleted in the MVP.
 */
@Service
public class CommentService {

	private final TaskService taskService;

	private final CommentRepository comments;

	private final AccountService accounts;

	private final Clock clock;

	CommentService(TaskService taskService, CommentRepository comments, AccountService accounts,
			Optional<Clock> clock) {
		this.taskService = taskService;
		this.comments = comments;
		this.accounts = accounts;
		this.clock = clock.orElseGet(Clock::systemUTC);
	}

	@Transactional(readOnly = true)
	public List<CommentView> listComments(long callerId, long projectId, long taskId) {
		this.taskService.requireTaskForMember(callerId, projectId, taskId);
		List<Comment> found = this.comments.findByTaskIdOrderByCreatedAtAscIdAsc(taskId);
		Map<Long, User> authors = this.accounts
			.findUsers(found.stream().map(Comment::getAuthorId).filter(Objects::nonNull).distinct().toList())
			.stream()
			.collect(Collectors.toMap(User::getId, Function.identity()));
		return found.stream().map(comment -> view(comment, authors.get(comment.getAuthorId()))).toList();
	}

	@Transactional
	public CommentView addComment(long callerId, long projectId, long taskId, String content) {
		this.taskService.requireTaskForMember(callerId, projectId, taskId);
		Comment comment = this.comments.save(new Comment(taskId, callerId, Text.trim(content), this.clock.instant()));
		return view(comment, this.accounts.findUser(callerId).orElse(null));
	}

	private static CommentView view(Comment comment, User author) {
		return new CommentView(comment.getId(), comment.getTaskId(), comment.getContent(),
				(author != null) ? UserSummary.of(author) : null, comment.getCreatedAt());
	}

}
