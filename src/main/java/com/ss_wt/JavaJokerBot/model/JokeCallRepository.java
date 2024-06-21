package com.ss_wt.JavaJokerBot.model;


import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JokeCallRepository extends CrudRepository<JokeCall, Long> {

}
