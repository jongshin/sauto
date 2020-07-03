package com.vkim.skyeng.service;

import com.vkim.skyeng.dto.CompanyDto;
import com.vkim.skyeng.entity.CompanyEntity;
import com.vkim.skyeng.mapper.BeanMapper;
import com.vkim.skyeng.repository.CompanyRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class CompanyService extends AbstractCrudService<CompanyDto, CompanyEntity> {

  private BeanMapper<CompanyDto, CompanyEntity> beanMapper;
  private CompanyRepository companyRepository;

  @Autowired
  public CompanyService(
      BeanMapper<CompanyDto, CompanyEntity> beanMapper,
      CompanyRepository companyRepository) {
    this.beanMapper = beanMapper;
    this.companyRepository = companyRepository;
  }

  @Override
  @Transactional
  public CompanyDto update(CompanyDto companyDto) {
    CompanyEntity companyEntity = companyRepository
        .findByCompanyNameOrderByIdAsc(companyDto.getCompanyName());
    if (companyEntity != null) {
      updateEntityWithDto(companyEntity, companyDto);
      return super.update(mapToDto(companyEntity));
    }
    return super.update(companyDto);
  }

  @Override
  protected BeanMapper<CompanyDto, CompanyEntity> getMapper() {
    return beanMapper;
  }

  @Override
  protected CrudRepository<CompanyEntity, Long> getRepository() {
    return companyRepository;
  }
}
