package com.acme.roombooking.building;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FloorRepository extends JpaRepository<Floor, Long> {
  @EntityGraph(attributePaths = "building")
  List<Floor> findByBuildingIdOrderByLevelAsc(long buildingId);

  @Override
  @EntityGraph(attributePaths = "building")
  Optional<Floor> findById(Long id);
}
