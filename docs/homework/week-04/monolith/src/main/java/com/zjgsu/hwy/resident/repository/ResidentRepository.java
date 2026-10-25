package com.zjgsu.hwy.resident.repository;
import com.zjgsu.hwy.resident.model.Resident;
import org.springframework.data.jpa.repository.JpaRepository;
public interface ResidentRepository extends JpaRepository<Resident,Long> {
 boolean existsByPhone(String phone);
}
