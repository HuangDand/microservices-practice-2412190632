package com.zjgsu.hwy.resident.controller;
import com.zjgsu.hwy.resident.dto.CreateResidentRequest;
import com.zjgsu.hwy.resident.model.Resident;
import com.zjgsu.hwy.resident.service.ResidentService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/residents")
public class ResidentController {
 private final ResidentService service;
 public ResidentController(ResidentService service){this.service=service;}
 @PostMapping public ResponseEntity<Resident> create(@Valid @RequestBody CreateResidentRequest r){
  Resident x=service.create(r);return ResponseEntity.created(URI.create("/api/residents/"+x.getId())).body(x);
 }
 @GetMapping public List<Resident> list(){return service.list();}
 @GetMapping("/{id}") public Resident get(@PathVariable Long id){return service.get(id);}
}
