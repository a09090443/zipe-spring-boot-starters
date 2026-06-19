package com.zipe.testsupport.repository;

import com.zipe.testsupport.entity.DummyEntity;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 測試專用的 JPA Repository，置於獨立套件 {@code com.zipe.testsupport.repository}，
 * 用以驗證 db-starter 能依 {@code dynamic.base-packages} 設定掃描並註冊此 Repository。
 */
public interface DummyRepository extends JpaRepository<DummyEntity, Long> {
}
