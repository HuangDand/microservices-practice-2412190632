package com.zjgsu.hwy.repair.repository;
import com.zjgsu.hwy.repair.model.*;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
public interface RepairTicketRepository extends JpaRepository<RepairTicket,Long> {
 List<RepairTicket> findByStatusOrderByCreatedAtDesc(RepairStatus status);
 List<RepairTicket> findByResidentIdOrderByCreatedAtDesc(Long residentId);
}
