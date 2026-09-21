package com.agatha.todo_api.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.agatha.todo_api.entity.Task;
import com.agatha.todo_api.entity.User;

public interface TaskRepository extends JpaRepository<Task, Long> {

	List<Task> findAllByOwner(User owner);

	Optional<Task> findByIdAndOwner(Long id, User owner);
}