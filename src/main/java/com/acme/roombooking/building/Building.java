package com.acme.roombooking.building;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "buildings")
public class Building {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, unique = true, length = 20)
  private String code;

  @Column(nullable = false, length = 100)
  private String name;

  @Column(nullable = false, length = 240)
  private String address;

  @Version private long version;

  protected Building() {}

  Building(String code, String name, String address) {
    this.code = code;
    update(name, address);
  }

  void update(String name, String address) {
    this.name = name.strip();
    this.address = address.strip();
  }

  public Long getId() {
    return id;
  }

  public String getCode() {
    return code;
  }

  public String getName() {
    return name;
  }

  public String getAddress() {
    return address;
  }
}
