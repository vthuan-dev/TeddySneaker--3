package com.web.application.service;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import com.web.application.entity.Promotion;
import com.web.application.model.request.CreatePromotionRequest;

import java.util.List;

@Service
public interface PromotionService {

    Page<Promotion> adminGetListPromotion(String code, String name, String publish, String active, int page);

    Promotion createPromotion(CreatePromotionRequest createPromotionRequest);

    void updatePromotion(CreatePromotionRequest createPromotionRequest, long id);

    void deletePromotion(long id);

    Promotion findPromotionById(long id);

    //Kiểm tra có khuyến mại
    Promotion checkPublicPromotion();

    //Tính giá sản phẩm khi có khuyến mại
    long calculatePromotionPrice(long price, Promotion promotion);

    //Lấy khuyến mại theo mã code
    Promotion checkPromotion(String code);

    //Lấy khuyến mại đang chạy và còn thời hạn
    List<Promotion> getAllValidPromotion();
}
