package com.acme.roombooking.room;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RoomRepository extends JpaRepository<Room, Long> {
  @EntityGraph(attributePaths = {"floor", "floor.building", "equipment"})
  @Query("select distinct r from Room r order by r.code")
  List<Room> findCatalog();

  @Override
  @EntityGraph(attributePaths = {"floor", "floor.building", "equipment"})
  Optional<Room> findById(Long id);

  // Every booking writer takes this lock before checking availability.
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select r from Room r where r.id = :id")
  Optional<Room> lockById(@Param("id") long id);
}
