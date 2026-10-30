package com.acme.roombooking.room;

import com.acme.roombooking.building.Floor;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "rooms")
public class Room {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "floor_id", nullable = false)
  private Floor floor;

  @Column(nullable = false, unique = true, length = 20)
  private String code;

  @Column(nullable = false, length = 100)
  private String name;

  @Column(nullable = false)
  private int capacity;

  @Column(nullable = false)
  private boolean active;

  @ElementCollection
  @CollectionTable(name = "room_equipment", joinColumns = @JoinColumn(name = "room_id"))
  @Enumerated(EnumType.STRING)
  @Column(name = "equipment", nullable = false, length = 30)
  private Set<Equipment> equipment = new HashSet<>();

  @Version private long version;

  protected Room() {}

  Room(Floor floor, RoomRequest request) {
    this.floor = floor;
    this.code = request.code();
    update(request);
  }

  void update(RoomRequest request) {
    this.name = request.name().strip();
    this.capacity = request.capacity();
    this.active = request.active();
    this.equipment.clear();
    this.equipment.addAll(request.equipment());
  }

  public Long getId() {
    return id;
  }

  public Floor getFloor() {
    return floor;
  }

  public String getCode() {
    return code;
  }

  public String getName() {
    return name;
  }

  public int getCapacity() {
    return capacity;
  }

  public boolean isActive() {
    return active;
  }

  public Set<Equipment> getEquipment() {
    return Set.copyOf(equipment);
  }
}
