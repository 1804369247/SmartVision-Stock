package com.example.smartvisionstock.repository;

import com.example.smartvisionstock.entity.WavePick;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WavePickRepository extends JpaRepository<WavePick, Long> {

    Optional<WavePick> findByWaveId(String waveId);

    List<WavePick> findByStatusIn(List<String> statuses);

    List<WavePick> findByStatus(String status);

    void deleteByWaveId(String waveId);
}
