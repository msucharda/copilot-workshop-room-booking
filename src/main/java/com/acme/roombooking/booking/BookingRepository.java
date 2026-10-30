package com.acme.roombooking.booking;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BookingRepository extends JpaRepository<Booking, UUID> {
  @Query(
      """
      select new com.acme.roombooking.booking.BusyInterval(b.startsAt, b.endsAt)
      from Booking b
      where b.room.id = :roomId
        and b.status = com.acme.roombooking.booking.BookingStatus.CONFIRMED
        and b.startsAt < :dayEndsAt and b.endsAt > :dayStartsAt
      order by b.startsAt
      """)
  List<BusyInterval> findBusyIntervals(
      @Param("roomId") long roomId,
      @Param("dayStartsAt") Instant dayStartsAt,
      @Param("dayEndsAt") Instant dayEndsAt);

  @Override
  @EntityGraph(attributePaths = {"room", "room.floor", "room.floor.building"})
  Optional<Booking> findById(UUID id);

  @EntityGraph(attributePaths = {"room", "room.floor", "room.floor.building"})
  @Query(
      """
      select b from Booking b
      where b.ownerUsername = :owner and (:status is null or b.status = :status)
      """)
  Page<Booking> findOwned(
      @Param("owner") String owner, @Param("status") BookingStatus status, Pageable pageable);

  @Query(
      """
      select b.room.id from Booking b
      where b.room.id in :roomIds
        and b.status = com.acme.roombooking.booking.BookingStatus.CONFIRMED
        and b.startsAt <= :endsAt
        and b.endsAt >= :startsAt
      """)
  List<Long> findConflictingRoomIds(
      @Param("roomIds") Collection<Long> roomIds,
      @Param("startsAt") Instant startsAt,
      @Param("endsAt") Instant endsAt);
}
