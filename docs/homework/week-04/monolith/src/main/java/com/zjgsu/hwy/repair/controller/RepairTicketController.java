package com.zjgsu.hwy.repair.controller;
import com.zjgsu.hwy.repair.dto.*;
import com.zjgsu.hwy.repair.model.*;
import com.zjgsu.hwy.repair.service.RepairTicketService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/repair-tickets")
public class RepairTicketController {
 private final RepairTicketService service;
 public RepairTicketController(RepairTicketService service){this.service=service;}
 @PostMapping public ResponseEntity<RepairTicket> create(@Valid @RequestBody CreateRepairTicketRequest r){
  RepairTicket x=service.create(r);return ResponseEntity.created(URI.create("/api/repair-tickets/"+x.getId())).body(x);
 }
 @GetMapping public List<RepairTicket> list(@RequestParam(required=false) RepairStatus status,@RequestParam(required=false) Long residentId){
  return service.list(status,residentId);
 }
 @GetMapping("/{id}") public RepairTicket get(@PathVariable Long id){return service.get(id);}
 @PatchMapping("/{id}/status") public RepairTicket update(@PathVariable Long id,@Valid @RequestBody UpdateRepairStatusRequest r){
  return service.updateStatus(id,r);
 }
}
