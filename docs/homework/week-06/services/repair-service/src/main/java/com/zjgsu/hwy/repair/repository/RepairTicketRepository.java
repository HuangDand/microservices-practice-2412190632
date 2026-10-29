package com.zjgsu.hwy.repair.repository;
import com.zjgsu.hwy.repair.model.*;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
public interface RepairTicketRepository extends JpaRepository<RepairTicket,Long> {
 List<RepairTicket> findByStatusOrderByCreatedAtDesc(RepairStatus status);
 List<RepairTicket> findByResidentIdOrderByCreatedAtDesc(Long residentId);
 @Query("select t from RepairTicket t where lower(t.title) like lower(concat('%',:kw,'%'))"
  +" or lower(t.description) like lower(concat('%',:kw,'%'))"
  +" or lower(t.location) like lower(concat('%',:kw,'%'))"
  +" order by t.createdAt desc")
 List<RepairTicket> search(@Param("kw") String kw);
}
