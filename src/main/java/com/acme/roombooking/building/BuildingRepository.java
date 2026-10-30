package com.acme.roombooking.building;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

interface BuildingRepository extends JpaRepository<Building, Long> {
  List<Building> findAllByOrderByCodeAsc();
}
