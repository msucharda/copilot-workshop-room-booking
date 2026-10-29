package com.acme.roombooking.booking;

import com.acme.roombooking.room.Room;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "bookings")
public class Booking {
  @Id private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "room_id", nullable = false)
  private Room room;

  @Column(nullable = false, length = 50)
  private String ownerUsername;

  @Column(nullable = false, length = 120)
  private String title;

  @Column(nullable = false)
  private int attendees;

  @Column(nullable = false)
  private Instant startsAt;

  @Column(nullable = false)
  private Instant endsAt;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 16)
  private BookingStatus status;

  @Column(nullable = false)
  private Instant createdAt;

  private Instant cancelledAt;

  @Version private long version;

  protected Booking() {}

  Booking(Room room, String ownerUsername, BookingRequest request, Instant now) {
    this.id = UUID.randomUUID();
    this.room = room;
    this.ownerUsername = ownerUsername;
    this.title = request.title().strip();
    this.attendees = request.attendees();
    this.startsAt = request.startsAt();
    this.endsAt = request.endsAt();
    this.status = BookingStatus.CONFIRMED;
    this.createdAt = now;
  }

  void cancel(Instant now) {
    this.status = BookingStatus.CANCELLED;
    this.cancelledAt = now;
  }

  void updateDetails(BookingUpdateRequest request) {
    this.title = request.title().strip();
    this.attendees = request.attendees();
  }

  public UUID getId() {
    return id;
  }

  public Room getRoom() {
    return room;
  }

  public String getOwnerUsername() {
    return ownerUsername;
  }

  public String getTitle() {
    return title;
  }

  public int getAttendees() {
    return attendees;
  }

  public Instant getStartsAt() {
    return startsAt;
  }

  public Instant getEndsAt() {
    return endsAt;
  }

  public BookingStatus getStatus() {
    return status;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getCancelledAt() {
    return cancelledAt;
  }
}
