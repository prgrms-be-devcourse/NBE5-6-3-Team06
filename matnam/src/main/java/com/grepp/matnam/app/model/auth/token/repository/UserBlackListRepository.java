package com.grepp.matnam.app.model.auth.token.repository;

import com.grepp.matnam.app.model.auth.token.entity.UserBlackList;
import org.springframework.data.repository.CrudRepository;

public interface UserBlackListRepository extends CrudRepository<UserBlackList, String> {
}