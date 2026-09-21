package com.agatha.todo_api.service;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.agatha.todo_api.dto.TaskRequest;
import com.agatha.todo_api.dto.TaskResponse;
import com.agatha.todo_api.entity.Task;
import com.agatha.todo_api.entity.User;
import com.agatha.todo_api.repository.TaskRepository;
import com.agatha.todo_api.repository.UserRepository;

@Service
public class TaskService {

	private final TaskRepository taskRepository;
	private final UserRepository userRepository;

	public TaskService(TaskRepository taskRepository, UserRepository userRepository) {
		this.taskRepository = taskRepository;
		this.userRepository = userRepository;
	}

	public List<TaskResponse> list() {
		return taskRepository.findAllByOwner(currentUser()).stream().map(TaskResponse::from).toList();
	}

	public TaskResponse get(Long id) {
		return TaskResponse.from(findOwned(id));
	}

	public TaskResponse create(TaskRequest request) {
		Task task = new Task(request.title(), request.description(), Boolean.TRUE.equals(request.done()),
				currentUser());
		return TaskResponse.from(taskRepository.save(task));
	}

	public TaskResponse update(Long id, TaskRequest request) {
		Task task = findOwned(id);
		task.setTitle(request.title());
		task.setDescription(request.description());
		task.setDone(Boolean.TRUE.equals(request.done()));
		return TaskResponse.from(taskRepository.save(task));
	}

	public void delete(Long id) {
		taskRepository.delete(findOwned(id));
	}

	/** Tarefa de outro usuario e tratada como inexistente (404). */
	private Task findOwned(Long id) {
		return taskRepository.findByIdAndOwner(id, currentUser())
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Task not found"));
	}

	private User currentUser() {
		String username = SecurityContextHolder.getContext().getAuthentication().getName();
		return userRepository.findByUsername(username)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
	}
}