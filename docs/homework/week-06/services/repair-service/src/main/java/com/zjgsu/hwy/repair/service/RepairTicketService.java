package com.zjgsu.hwy.repair.service;
import com.zjgsu.hwy.common.ResourceNotFoundException;
import com.zjgsu.hwy.repair.dto.*;
import com.zjgsu.hwy.repair.model.*;
import com.zjgsu.hwy.repair.repository.RepairTicketRepository;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
@Service
public class RepairTicketService {
 private final RepairTicketRepository tickets;
 private final RestTemplate restTemplate;
 private final String residentUrl;
 public RepairTicketService(RepairTicketRepository t,RestTemplate rt,@Value("${services.resident-url}") String residentUrl){
  tickets=t;restTemplate=rt;this.residentUrl=residentUrl;
 }
 private void checkResidentExists(Long residentId){
  try{
   restTemplate.getForObject(residentUrl+"/api/residents/"+residentId,Object.class);
  }catch(HttpClientErrorException e){
   if(e.getStatusCode()==HttpStatus.NOT_FOUND)throw new ResourceNotFoundException("业主不存在: "+residentId);
   throw new RuntimeException("调用业主服务失败: "+e.getMessage(),e);
  }
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
