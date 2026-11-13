package com.zjgsu.hwy.resident.controller;
import com.zjgsu.hwy.resident.dto.CreateResidentRequest;
import com.zjgsu.hwy.resident.dto.UpdateResidentRequest;
import com.zjgsu.hwy.resident.model.Resident;
import com.zjgsu.hwy.resident.service.ResidentService;
import com.zjgsu.hwy.system.web.SystemStatusController;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/residents")
public class ResidentController {
 private final ResidentService service;
 private final SystemStatusController sysStatus;
 public ResidentController(ResidentService s, SystemStatusController sys){this.service=s;this.sysStatus=sys;}

 private void writeInstanceHeader(HttpServletResponse resp){ resp.setHeader("X-Instance-Id", sysStatus.currentInstanceId()); }

 @PostMapping public ResponseEntity<Resident> create(@Valid @RequestBody CreateResidentRequest r, HttpServletResponse resp){
  Resident x=service.create(r); writeInstanceHeader(resp); return ResponseEntity.created(URI.create("/api/residents/"+x.getId())).body(x);
 }
 @GetMapping public List<Resident> list(@RequestParam(required=false) String keyword){return service.list(keyword);}
 @GetMapping("/{id}") public Resident get(@PathVariable Long id, HttpServletResponse resp){
  Resident x=service.get(id); writeInstanceHeader(resp); return x;
 }
 @PatchMapping("/{id}") public Resident update(@PathVariable Long id,@Valid @RequestBody UpdateResidentRequest r){
  return service.update(id,r);
 }
 @DeleteMapping("/{id}") public ResponseEntity<Void> delete(@PathVariable Long id){
  service.delete(id);return ResponseEntity.noContent().build();
 }
}
