package com.nhat.SharedBudgetManagement.repository;

import com.nhat.SharedBudgetManagement.entity.Tag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface TagRepository extends JpaRepository<Tag, Long> {

    Optional<Tag> findByName(String name);

    boolean existsByName(String name);

    List<Tag> findAllByNameIn(List<String> names);
}
