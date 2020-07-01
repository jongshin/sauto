package com.vkim.skyeng.repository;

import com.vkim.skyeng.entity.DictionaryEntity;
import java.util.List;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DictionaryRepository extends CrudRepository<DictionaryEntity, Long> {

  List<DictionaryEntity> findByDictionaryAndKeyOrderByIdAsc(String dictionary, String key);
}
