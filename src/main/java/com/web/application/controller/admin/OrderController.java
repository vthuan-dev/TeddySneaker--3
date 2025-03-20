package com.web.application.controller.admin;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import com.web.application.config.Contant;
import com.web.application.dto.OrderDetailDTO;
import com.web.application.dto.OrderInfoDTO;
import com.web.application.dto.ShortProductInfoDTO;
import com.web.application.dto.DetailProductInfoDTO;
import com.web.application.entity.Order;
import com.web.application.entity.Promotion;
import com.web.application.entity.User;
import com.web.application.entity.OrderItem;
import com.web.application.exception.BadRequestExp;
import com.web.application.exception.NotFoundExp;
import com.web.application.model.request.CreateOrderRequest;
import com.web.application.model.request.UpdateDetailOrder;
import com.web.application.model.request.UpdateStatusOrderRequest;
import com.web.application.security.CustomUserDetails;
import com.web.application.service.OrderService;
import com.web.application.service.ProductService;
import com.web.application.service.PromotionService;
import com.web.application.model.request.CheckoutRequestDTO;

import javax.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;
import java.util.Collections;

@Controller
public class OrderController {

	@Autowired
	private OrderService orderService;

	@Autowired
	private ProductService productService;

	@Autowired
	private PromotionService promotionService;

	@GetMapping("/admin/orders")
	public String getListOrderPage(Model model, @RequestParam(defaultValue = "", required = false) String id,
			@RequestParam(defaultValue = "", required = false) String name,
			@RequestParam(defaultValue = "", required = false) String phone,
			@RequestParam(defaultValue = "", required = false) String status,
			@RequestParam(defaultValue = "", required = false) String product,
			@RequestParam(defaultValue = "", required = false) String createdAt,
			@RequestParam(defaultValue = "", required = false) String modifiedAt,
			@RequestParam(defaultValue = "1") Integer page) {

		// Lấy danh sách sản phẩm
		List<ShortProductInfoDTO> productList = productService.getListProduct();
		model.addAttribute("productList", productList);

		// Lấy danh sách đơn hàng
		Page<Order> orderPage = orderService.adminGetListOrders(id, name, phone, status, product, createdAt, modifiedAt,
				page);
		
		// Thêm logging
		System.out.println("Total orders: " + orderPage.getTotalElements());
		System.out.println("Current page content size: " + orderPage.getContent().size());
		
		// In ra một vài đơn hàng để kiểm tra
		orderPage.getContent().forEach(order -> {
			System.out.println("Order ID: " + order.getId() + ", Receiver: " + order.getReceiverName());
		});
		
		model.addAttribute("orderPage", orderPage.getContent());
		model.addAttribute("totalPages", orderPage.getTotalPages());
		model.addAttribute("currentPage", orderPage.getPageable().getPageNumber() + 1);

		return "admin/order/list";
	}

	@GetMapping("/admin/orders/create")
	public String createOrderPage(Model model) {

		// Get list product
		List<ShortProductInfoDTO> products = productService.getAvailableProducts();
		model.addAttribute("products", products);

		// Get list size
		model.addAttribute("sizeVn", Contant.SIZE_VN);

//        //Get list valid promotion
		List<Promotion> promotions = promotionService.getAllValidPromotion();
		model.addAttribute("promotions", promotions);
		return "admin/order/create";
	}

	@PostMapping("/api/admin/orders")
	public ResponseEntity<Object> createOrder(@Valid @RequestBody CreateOrderRequest createOrderRequest) {
		User user = ((CustomUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal())
				.getUser();
		Order order = orderService.createOrderAdmin(createOrderRequest, user.getId());
		return ResponseEntity.ok(order);
	}

	@GetMapping("/admin/orders/update/{id}")
	public String updateOrderPage(Model model, @PathVariable long id) {
		Order order = orderService.findOrderById(id);
		model.addAttribute("order", order);

		if (order.getStatus() == Contant.ORDER_STATUS) {
			// Get list product to select
			List<ShortProductInfoDTO> products = productService.getAvailableProducts();
			model.addAttribute("products", products);

			// Get list valid promotion
			List<Promotion> promotions = promotionService.getAllValidPromotion();
			model.addAttribute("promotions", promotions);
			if (order.getPromotion() != null) {
				boolean validPromotion = false;
				for (Promotion promotion : promotions) {
					if (promotion.getCouponCode().equals(order.getPromotion().getCouponCode())) {
						validPromotion = true;
						break;
					}
				}
				if (!validPromotion) {
					promotions.add(new Promotion(order.getPromotion()));
				}
			}

			// Check size available for each order item
			for (OrderItem item : order.getItems()) {
				boolean sizeIsAvailable = productService.checkProductSizeAvailable(
					item.getProduct().getId(),
					item.getSize()
				);
				model.addAttribute("sizeIsAvailable_" + item.getId(), sizeIsAvailable);
			}
		}

		return "admin/order/edit";
	}

	@PutMapping("/api/admin/orders/update-detail/{id}")
	public ResponseEntity<Object> updateDetailOrder(@Valid @RequestBody UpdateDetailOrder detailOrder,
			@PathVariable long id) {
		User user = ((CustomUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal())
				.getUser();
		orderService.updateDetailOrder(detailOrder, id, user.getId());
		return ResponseEntity.ok("Cập nhật thành công");
	}

	@PutMapping("/api/admin/orders/update-status/{id}")
	public ResponseEntity<Object> updateStatusOrder(
			@Valid @RequestBody UpdateStatusOrderRequest updateStatusOrderRequest, @PathVariable long id) {
		User user = ((CustomUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal())
				.getUser();
		orderService.updateStatusOrder(updateStatusOrderRequest, id, user.getId());
		return ResponseEntity.ok("Cập nhật trạng thái thành công");
	}

	@GetMapping("/tai-khoan/lich-su-giao-dich")
	public String getOrderHistoryPage(Model model) {
		try {
			// Get user information
			Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
			if (!(authentication.getPrincipal() instanceof CustomUserDetails)) {
				return "redirect:/login";
			}
			
			CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
			User user = userDetails.getUser();

			// Get all orders for initial display - pass Contant.ORDER_STATUS instead of null
			List<OrderInfoDTO> orderList = orderService.getListOrderOfPersonByStatus(Contant.ORDER_STATUS, user.getId());
			
			// Add attributes to model
			model.addAttribute("orderList", orderList);
			model.addAttribute("user_fullname", user.getFullName());
			
			return "shop/order_history";
		} catch (Exception e) {
			e.printStackTrace();
			return "error/500";
		}
	}

	@GetMapping("/api/get-order-list")
	@ResponseBody
	public ResponseEntity<Object> getListOrderByStatus(@RequestParam Integer status) {
		try {
			// Validate status
			if (!Contant.LIST_ORDER_STATUS.contains(status)) {
				return ResponseEntity.badRequest().body("Trạng thái đơn hàng không hợp lệ");
			}

			Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
			if (!(authentication.getPrincipal() instanceof CustomUserDetails)) {
				return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Chưa đăng nhập");
			}

			CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
			User user = userDetails.getUser();
			List<OrderInfoDTO> orders = orderService.getListOrderOfPersonByStatus(status, user.getId());

			return ResponseEntity.ok(orders);
		} catch (Exception e) {
			e.printStackTrace();
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
							   .body("Lỗi: " + e.getMessage());
		}
	}

	@GetMapping("/tai-khoan/lich-su-giao-dich/{id}")
	public String getDetailOrderPage(Model model, @PathVariable long id) {
		try {
			// Get user information
			Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
			if (!(authentication.getPrincipal() instanceof CustomUserDetails)) {
				return "redirect:/login";
			}
			
			CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
			User user = userDetails.getUser();

			// Get order detail
			OrderDetailDTO order = orderService.userGetDetailById(id, user.getId());
			if (order == null) {
				return "error/404";
			}

			// Add order to model
			model.addAttribute("order", order);
			
			// Check if order can be cancelled
			if (order.getStatus() == Contant.ORDER_STATUS) {
				model.addAttribute("canCancel", true);
			} else {
				model.addAttribute("canCancel", false);
			}

			return "shop/order-detail";
		} catch (Exception e) {
			e.printStackTrace();
			return "error/500";
		}
	}

	@PostMapping("/api/return-order/{id}")
	public ResponseEntity<Object> returnOrder(@PathVariable long id,
			@Valid @RequestBody UpdateStatusOrderRequest updateStatusOrderRequest) {
		User user = ((CustomUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal())
				.getUser();

		orderService.userReturnOrder(id, user.getId(), updateStatusOrderRequest);

		return ResponseEntity.ok("Trả đơn hàng thành công");
	}

	@PostMapping("/api/cancel-order/{id}")
	public ResponseEntity<Object> cancelOrder(@PathVariable long id) {
		User user = ((CustomUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal())
				.getUser();

		orderService.userCancelOrder(id, user.getId());

		return ResponseEntity.ok("Hủy đơn hàng thành công");
	}

	@GetMapping("/admin/checkout") 
	public String getCheckoutPage(Model model, 
			@RequestParam String id,
			@RequestParam int size) {
		
		// Kiểm tra sản phẩm và size
		try {
			DetailProductInfoDTO product = 	productService.getDetailProductById(id);
			model.addAttribute("product", product);
		} catch (NotFoundExp ex) {
			return "error/404";
		}

		// Validate size
		if (size < 35 || size > 42) {
			return "error/404";
		}

		// Kiểm tra size có sẵn
		List<Integer> availableSizes = productService.getListAvailableSize(id);
		model.addAttribute("availableSizes", availableSizes);
		
		boolean notFoundSize = true;
		for (Integer availableSize : availableSizes) {
			if (availableSize == size) {
				notFoundSize = false;
				break;
			}
		}
		model.addAttribute("notFoundSize", notFoundSize);

		// Thêm thông tin user
		User user = ((CustomUserDetails) SecurityContextHolder.getContext()
			.getAuthentication().getPrincipal()).getUser();
		model.addAttribute("user_fullname", user.getFullName());
		model.addAttribute("user_phone", user.getPhone());
		model.addAttribute("size", size);

		return "shop/payment";
	}

	@GetMapping("/admin/orders/{id}/detail")
	public String getOrderDetailPage(Model model, @PathVariable long id) {
		Order order = orderService.findOrderById(id);
		
		// Lấy thông tin chi tiết đơn hàng
		model.addAttribute("order", order);
		
		// Lấy danh sách sản phẩm trong đơn hàng
		model.addAttribute("orderItems", order.getItems());
		
		// Lấy danh sách size
		model.addAttribute("sizeVn", Contant.SIZE_VN);
		
		// Lấy danh sách khuyến mãi hợp lệ
		List<Promotion> promotions = promotionService.getAllValidPromotion();
		model.addAttribute("promotions", promotions);
		
		return "admin/order/detail";
	}

	@PostMapping("/api/admin/orders/{id}/update")
	public ResponseEntity<?> updateDetailOrder(@PathVariable long id, @RequestBody UpdateDetailOrder updateDetailOrder) {
		try {
			User user = ((CustomUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getUser();
			orderService.updateDetailOrder(updateDetailOrder, id, user.getId());
			return ResponseEntity.ok("Cập nhật thành công");
		} catch (Exception e) {
			return ResponseEntity.badRequest().body(e.getMessage());
		}
	}

	@PostMapping("/api/orders/cart")
	public ResponseEntity<Object> createOrderFromCart(@RequestBody CheckoutRequestDTO request) {
		User user = ((CustomUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal())
				.getUser();
		Order order = orderService.createOrderFromCart(user.getId(), request);
		return ResponseEntity.ok(order);
	}

	@GetMapping("/api/admin/orders/list")
	@ResponseBody
	public ResponseEntity<Map<String, Object>> getOrdersList(
			@RequestParam(defaultValue = "") String id,
			@RequestParam(defaultValue = "") String name,
			@RequestParam(defaultValue = "") String phone,
			@RequestParam(defaultValue = "") String status,
			@RequestParam(defaultValue = "") String product,
			@RequestParam(defaultValue = "") String createdAt,
			@RequestParam(defaultValue = "") String modifiedAt,
			@RequestParam(defaultValue = "1") Integer page) {

		try {
			// Add logging
			System.out.println("Getting orders with params:");
			System.out.println("Page: " + page);
			System.out.println("ID: " + id);
			System.out.println("Name: " + name);
			System.out.println("Phone: " + phone);
			System.out.println("Status: " + status);
			
			Page<Order> orderPage = orderService.adminGetListOrders(
				id, name, phone, status, product, createdAt, modifiedAt, page);

			// Add logging
			System.out.println("Found total: " + orderPage.getTotalElements());
			System.out.println("Total pages: " + orderPage.getTotalPages());
			System.out.println("Current page content size: " + orderPage.getContent().size());

			Map<String, Object> response = new HashMap<>();
			response.put("content", orderPage.getContent());
			response.put("totalPages", orderPage.getTotalPages());
			response.put("currentPage", page);
			response.put("totalElements", orderPage.getTotalElements());

			return ResponseEntity.ok(response);
		} catch (Exception e) {
			e.printStackTrace();
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(Collections.singletonMap("error", e.getMessage()));
		}
	}

}
