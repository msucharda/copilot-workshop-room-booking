package com.acme.roombooking.building;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "floors")
public class Floor {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "building_id", nullable = false)
  private Building building;

  @Column(nullable = false)
  private int level;

  @Column(nullable = false, length = 100)
  private String name;

  protected Floor() {}

  Floor(Building building, int level, String name) {
    this.building = building;
    this.level = level;
    this.name = name.strip();
  }

  public Long getId() {
    return id;
  }

  public Building getBuilding() {
    return building;
  }

  public int getLevel() {
    return level;
  }

  public String getName() {
    return name;
  }
}
