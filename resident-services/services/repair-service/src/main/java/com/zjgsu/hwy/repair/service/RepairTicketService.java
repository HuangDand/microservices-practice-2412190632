package com.zjgsu.hwy.repair.service;
import com.zjgsu.hwy.common.ResourceNotFoundException;
import com.zjgsu.hwy.repair.client.ResidentClient;
import com.zjgsu.hwy.repair.dto.*;
import com.zjgsu.hwy.repair.model.*;
import com.zjgsu.hwy.repair.repository.RepairTicketRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service
public class RepairTicketService {
 private final RepairTicketRepository tickets;
 private final ResidentClient residentClient;
 public RepairTicketService(RepairTicketRepository t,ResidentClient residentClient){
  tickets=t;this.residentClient=residentClient;
 }
 private void checkResidentExists(Long residentId){
  residentClient.ensureExists(residentId);
 }
 @Transactional public RepairTicket create(CreateRepairTicketRequest r){
  checkResidentExists(r.residentId());
  return tickets.save(new RepairTicket(r.title().trim(),r.description().trim(),r.location().trim(),r.residentId()));
 }
 @Transactional(readOnly=true) public List<RepairTicket> list(RepairStatus status,Long residentId,String keyword){
  if(keyword!=null&&!keyword.isBlank())return tickets.search(keyword.trim());
  if(residentId!=null){checkResidentExists(residentId);return tickets.findByResidentIdOrderByCreatedAtDesc(residentId);}
  if(status!=null)return tickets.findByStatusOrderByCreatedAtDesc(status);
  return tickets.findAll();
 }
 @Transactional(readOnly=true) public RepairTicket get(Long id){
  return tickets.findById(id).orElseThrow(()->new ResourceNotFoundException("报修工单不存在: "+id));
 }
 @Transactional public RepairTicket updateStatus(Long id,UpdateRepairStatusRequest r){
  RepairTicket ticket=get(id); RepairStatus old=ticket.getStatus(), next=r.status();
  boolean allowed=(old==RepairStatus.OPEN&&next==RepairStatus.IN_PROGRESS)
   ||(old==RepairStatus.IN_PROGRESS&&next==RepairStatus.RESOLVED);
  if(!allowed)throw new IllegalArgumentException("非法工单状态流转："+old+" -> "+next+"；允许 OPEN -> IN_PROGRESS -> RESOLVED");
  ticket.changeStatus(next);return tickets.save(ticket);
 }
 @Transactional public RepairTicket update(Long id,UpdateRepairTicketRequest r){
  RepairTicket ticket=get(id);
  ticket.update(
   r.title()!=null?r.title().trim():ticket.getTitle(),
   r.description()!=null?r.description().trim():ticket.getDescription(),
   r.location()!=null?r.location().trim():ticket.getLocation());
  return tickets.save(ticket);
 }
 @Transactional public void delete(Long id){
  tickets.delete(get(id));
 }
}
