package com.ss_wt.JavaJokerBot.model;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface JokesRepository extends CrudRepository <Jokes, Long> {

    long count();
    Optional<Jokes> findById(Long id);

    List<Jokes> findTop5ByOrderByPopularityDesc();

    Page<Jokes> findAll(Pageable pageable);

}
