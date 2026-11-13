package com.zjgsu.hwy.resident.repository;
import com.zjgsu.hwy.resident.model.Resident;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
public interface ResidentRepository extends JpaRepository<Resident,Long> {
 boolean existsByPhone(String phone);
 boolean existsByPhoneAndIdNot(String phone,Long id);
 @Query("select r from Resident r where lower(r.fullName) like lower(concat('%',:kw,'%'))"
  +" or r.phone like concat('%',:kw,'%')"
  +" or lower(r.building) like lower(concat('%',:kw,'%'))"
  +" or lower(r.unitNumber) like lower(concat('%',:kw,'%'))"
  +" order by r.id")
 List<Resident> search(@Param("kw") String kw);
}
