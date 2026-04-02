package com.matheus.queue.repository;

import com.matheus.queue.model.Status;
import com.matheus.queue.model.Task;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TaskRepository extends JpaRepository<Task, Long> {

    List<Task> findByStatus(Status status);
}