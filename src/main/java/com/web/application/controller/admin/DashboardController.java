package com.web.application.controller.admin;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.http.HttpStatus;

import com.web.application.dto.ChartDTO;
import com.web.application.dto.StatisticDTO;
import com.web.application.entity.Order;
import com.web.application.model.request.FilterDayByDay;
import com.web.application.repository.*;
import com.web.application.service.*;

import java.time.LocalDate;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

@Controller
public class DashboardController {

    @Autowired
    private PostService postService;

    @Autowired
    private ProductService productService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private BrandService brandService;

    @Autowired
    private StatisticRepository statisticRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private BrandRepository brandRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private UserRepository userRepository;

    @GetMapping("/admin")
    public String dashboard(Model model){
        return "admin/index";
    }

    @GetMapping("/api/admin/count/posts")
    public ResponseEntity<Object> getCountPost(){
        long countPosts = postService.getCountPost();
        return ResponseEntity.ok(countPosts);
    }

    @GetMapping("/api/admin/count/products")
    public ResponseEntity<Object> getCountProduct(){
        long countProducts = productService.getCountProduct();
        return ResponseEntity.ok(countProducts);
    }

    @GetMapping("/api/admin/count/orders")
    public ResponseEntity<Object> getCountOrders(){
        long countOrders = orderService.getCountOrder();
        return ResponseEntity.ok(countOrders);
    }

    @GetMapping("/api/admin/count/categories")
    public ResponseEntity<Object> getCountCategories(){
        long countCategories = categoryService.getCountCategories();
        return ResponseEntity.ok(countCategories);
    }

    @GetMapping("/api/admin/count/brands")
    public ResponseEntity<Object> getCountBrands(){
        long countBrands = brandService.getCountBrands();
        return ResponseEntity.ok(countBrands);
    }

    @GetMapping("/api/admin/count/users")
    public ResponseEntity<Object> getCountUsers(){
        long countUsers = userRepository.count();
        return ResponseEntity.ok(countUsers);
    }

    @GetMapping("/api/admin/statistics")
    public ResponseEntity<Object> getStatistic30Day(){
        try {
            List<StatisticDTO> statistics = statisticRepository.getStatistic30Day();
            return ResponseEntity.ok(statistics);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                             .body("Error fetching statistics: " + e.getMessage());
        }
    }

    @PostMapping("/api/admin/statistics")
    public ResponseEntity<Object> getStatisticDayByDay(@RequestBody FilterDayByDay filterDayByDay){
        try {
            List<StatisticDTO> statisticDTOS = statisticRepository.getStatisticDayByDay(
                filterDayByDay.getToDate(),
                filterDayByDay.getFromDate()
            );
            return ResponseEntity.ok(statisticDTOS);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                             .body("Error fetching statistics: " + e.getMessage() + "\n" + e.getStackTrace()[0]);
        }
    }

    @GetMapping("/api/admin/product-order-categories")
    public ResponseEntity<Object> getListProductOrderCategories(){
        try {
            List<ChartDTO> chartDTOS = categoryRepository.getListProductOrderCategories();
            return ResponseEntity.ok(chartDTOS);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                             .body("Error fetching category statistics: " + e.getMessage());
        }
    }

    @GetMapping("/api/admin/product-order-brands")
    public ResponseEntity<Object> getProductOrderBrands(){
        try {
            List<ChartDTO> chartDTOS = brandRepository.getProductOrderBrands();
            return ResponseEntity.ok(chartDTOS);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                             .body("Error fetching brand statistics: " + e.getMessage());
        }
    }

    @GetMapping("/api/admin/product-order")
    public ResponseEntity<Object> getProductOrder(){
        try {
            Pageable pageable = PageRequest.of(0,10);
            LocalDate now = LocalDate.now();
            List<ChartDTO> chartDTOS = productRepository.getProductOrders(
                pageable, 
                now.getMonthValue(),
                now.getYear()
            );
            return ResponseEntity.ok(chartDTOS);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                             .body("Error fetching product orders: " + e.getMessage());
        }
    }

    @GetMapping("/api/admin/orders")
    public ResponseEntity<Object> getOrders() {
        try {
            List<Order> orders = orderService.getAllOrders();
            return ResponseEntity.ok(orders);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                               .body("Error fetching orders: " + e.getMessage());
        }
    }

    @GetMapping("/api/admin/orders/stats")
    public ResponseEntity<Object> getOrderStats() {
        try {
            Map<String, Long> stats = new HashMap<>();
            stats.put("pending", orderService.countOrdersByStatus(0));
            stats.put("confirmed", orderService.countOrdersByStatus(1));
            stats.put("shipping", orderService.countOrdersByStatus(2));
            stats.put("completed", orderService.countOrdersByStatus(3));
            stats.put("cancelled", orderService.countOrdersByStatus(4));
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                               .body("Error fetching order stats: " + e.getMessage());
        }
    }
}
