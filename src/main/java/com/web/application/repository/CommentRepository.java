package com.web.application.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.web.application.entity.Comment;

@Repository
public interface CommentRepository extends JpaRepository<Comment,Long> {

}
