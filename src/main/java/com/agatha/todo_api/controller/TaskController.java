package com.agatha.todo_api.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.agatha.todo_api.dto.TaskRequest;
import com.agatha.todo_api.dto.TaskResponse;
import com.agatha.todo_api.service.TaskService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/tasks")
public class TaskController {

	private final TaskService taskService;

	public TaskController(TaskService taskService) {
		this.taskService = taskService;
	}

	@GetMapping
	public List<TaskResponse> list() {
		return taskService.list();
	}

	@GetMapping("/{id}")
	public TaskResponse get(@PathVariable Long id) {
		return taskService.get(id);
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public TaskResponse create(@Valid @RequestBody TaskRequest request) {
		return taskService.create(request);
	}

	@PutMapping("/{id}")
	public TaskResponse update(@PathVariable Long id, @Valid @RequestBody TaskRequest request) {
		return taskService.update(id, request);
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void delete(@PathVariable Long id) {
		taskService.delete(id);
	}
}