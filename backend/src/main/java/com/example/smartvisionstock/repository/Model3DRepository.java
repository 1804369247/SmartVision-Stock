package com.example.smartvisionstock.repository;

import com.example.smartvisionstock.entity.Model3D;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface Model3DRepository extends JpaRepository<Model3D, Long> {

    Optional<Model3D> findByModelId(String modelId);

    @Query("SELECT m FROM Model3D m WHERE (:name IS NULL OR LOWER(m.name) LIKE LOWER(CONCAT('%', :name, '%'))) ORDER BY m.createTime DESC")
    List<Model3D> findByNameContaining(@Param("name") String name, Pageable pageable);

    @Query("SELECT COUNT(m) FROM Model3D m WHERE (:name IS NULL OR LOWER(m.name) LIKE LOWER(CONCAT('%', :name, '%')))")
    long countByNameContaining(@Param("name") String name);

    void deleteByModelId(String modelId);
}
