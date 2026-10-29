package com.zjgsu.hwy.resident.service;
import com.zjgsu.hwy.common.ResourceNotFoundException;
import com.zjgsu.hwy.resident.dto.CreateResidentRequest;
import com.zjgsu.hwy.resident.dto.UpdateResidentRequest;
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
 @Transactional(readOnly=true) public List<Resident> list(String keyword){
  if(keyword==null||keyword.isBlank()) return residents.findAll();
  return residents.search(keyword.trim());
 }
 @Transactional(readOnly=true) public Resident get(Long id){
  return residents.findById(id).orElseThrow(()->new ResourceNotFoundException("业主不存在: "+id));
 }
 @Transactional public Resident update(Long id,UpdateResidentRequest r){
  Resident resident=get(id);
  String phone=r.phone()!=null?r.phone().trim():resident.getPhone();
  if(!phone.equals(resident.getPhone())&&residents.existsByPhoneAndIdNot(phone,id))
   throw new IllegalArgumentException("该手机号已登记");
  resident.update(
   r.fullName()!=null?r.fullName().trim():resident.getFullName(),
   phone,
   r.building()!=null?r.building().trim():resident.getBuilding(),
   r.unitNumber()!=null?r.unitNumber().trim():resident.getUnitNumber());
  return residents.save(resident);
 }
 @Transactional public void delete(Long id){
  residents.delete(get(id));
 }
}
