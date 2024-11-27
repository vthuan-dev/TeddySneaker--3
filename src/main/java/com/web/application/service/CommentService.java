package com.web.application.service;

import org.springframework.stereotype.Service;

import com.web.application.entity.Comment;
import com.web.application.model.request.CreateCommentPostRequest;
import com.web.application.model.request.CreateCommentProductRequest;

@Service
public interface CommentService {
    Comment createCommentPost(CreateCommentPostRequest createCommentPostRequest, long userId);
    Comment createCommentProduct(CreateCommentProductRequest createCommentProductRequest, long userId);
}
