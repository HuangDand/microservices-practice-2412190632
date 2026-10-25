package com.zjgsu.hwy.resident.service;
import com.zjgsu.hwy.common.ResourceNotFoundException;
import com.zjgsu.hwy.resident.dto.CreateResidentRequest;
import com.zjgsu.hwy.resident.model.Resident;
import com.zjgsu.hwy.resident.repository.ResidentRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service
public class ResidentService {
 private final ResidentRepository residents;
 public ResidentService(ResidentRepository residents){this.residents=residents;}
 @Transactional public Resident create(CreateResidentRequest r){
  String phone=r.phone().trim();
  if(residents.existsByPhone(phone)) throw new IllegalArgumentException("该手机号已登记");
  return residents.save(new Resident(r.fullName().trim(),phone,r.building().trim(),r.unitNumber().trim()));
 }
 @Transactional(readOnly=true) public List<Resident> list(){return residents.findAll();}
 @Transactional(readOnly=true) public Resident get(Long id){
  return residents.findById(id).orElseThrow(()->new ResourceNotFoundException("业主不存在: "+id));
 }
}
