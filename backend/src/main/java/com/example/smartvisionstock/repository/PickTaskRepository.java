package com.example.smartvisionstock.repository;

import com.example.smartvisionstock.entity.PickTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PickTaskRepository extends JpaRepository<PickTask, Long> {

    Optional<PickTask> findByTaskId(String taskId);

    List<PickTask> findByWaveId(String waveId);

    List<PickTask> findByWaveIdAndStatus(String waveId, String status);

    void deleteByWaveId(String waveId);
}
