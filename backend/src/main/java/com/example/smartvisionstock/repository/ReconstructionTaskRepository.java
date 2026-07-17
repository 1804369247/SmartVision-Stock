package com.example.smartvisionstock.repository;

import com.example.smartvisionstock.entity.ReconstructionTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReconstructionTaskRepository extends JpaRepository<ReconstructionTask, Long> {

    Optional<ReconstructionTask> findByTaskId(String taskId);

    List<ReconstructionTask> findByStatus(String status);

    List<ReconstructionTask> findByUploadId(String uploadId);
}
