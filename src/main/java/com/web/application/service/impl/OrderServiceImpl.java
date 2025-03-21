package com.web.application.service.impl;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import java.util.ArrayList;
import java.util.stream.Collectors;
import java.util.Map;
import java.util.HashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.web.application.config.Contant;
import com.web.application.dto.OrderDetailDTO;
import com.web.application.dto.OrderInfoDTO;
import com.web.application.entity.Order;
import com.web.application.entity.Product;
import com.web.application.entity.ProductSize;
import com.web.application.entity.Promotion;
import com.web.application.entity.Statistic;
import com.web.application.entity.User;
import com.web.application.exception.BadRequestExp;
import com.web.application.exception.InternalServerExp;
import com.web.application.exception.NotFoundExp;
import com.web.application.model.request.CreateOrderRequest;
import com.web.application.model.request.UpdateDetailOrder;
import com.web.application.model.request.UpdateStatusOrderRequest;
import com.web.application.repository.OrderRepository;
import com.web.application.repository.ProductRepository;
import com.web.application.repository.ProductSizeRepository;
import com.web.application.repository.StatisticRepository;
import com.web.application.service.InvoiceService;
import com.web.application.service.OrderService;
import com.web.application.service.PromotionService;
import com.web.application.model.request.CreateInvoiceRequest;
import com.web.application.entity.Invoice;
import com.web.application.entity.OrderItem;
import com.web.application.service.CartService;
import com.web.application.entity.Cart;
import com.web.application.entity.CartItem;
import com.web.application.model.request.CheckoutRequestDTO;
import org.springframework.stereotype.Service;
import com.web.application.repository.UserRepository;
import com.web.application.dto.OrderItemDTO;
import org.springframework.security.access.AccessDeniedException;

@Service
public class OrderServiceImpl implements OrderService {

	@Autowired
	private ProductRepository productRepository;

	@Autowired
	private ProductSizeRepository productSizeRepository;

	@Autowired
	private OrderRepository orderRepository;

	@Autowired
	private PromotionService promotionService;

	@Autowired
	private StatisticRepository statisticRepository;

	@Autowired
	private InvoiceService invoiceService;

	@Autowired
	private CartService cartService;

	@Autowired
	private UserRepository userRepository;

	@Override
	public Page<Order> adminGetListOrders(String id, String name, String phone, String status, String product,
			String createdAt, String modifiedAt, int page) {
		try {
			page = (page <= 0) ? 0 : page - 1;
			Pageable pageable = PageRequest.of(page, 10, Sort.by("createdAt").descending());
			
			Page<Order> orders = orderRepository.findByConditions(id, name, phone, status, product, createdAt, modifiedAt, pageable);
			
			// Đảm bảo eager loading cho các order items và product
			orders.getContent().forEach(order -> {
				if (order.getItems() != null) {
					order.getItems().forEach(item -> {
						if (item.getProduct() != null) {
							item.getProduct().getName();  // Access để trigger loading
						}
					});
				}
			});
			
			return orders;
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException("Lỗi khi lấy danh sách đơn hàng: " + e.getMessage());
		}
	}

	@Override
	public Order createOrderAdmin(CreateOrderRequest createOrderRequest, long userId) {
		try {
			// Kiểm tra sản phẩm có tồn tại
			Optional<Product> product = productRepository.findById(createOrderRequest.getProductId());
			if (product.isEmpty()) {
				throw new NotFoundExp("Sản phẩm không tồn tại!");
			}

			// Kiểm tra size có sẵn
			ProductSize productSize = productSizeRepository.checkProductAndSizeAvailable(
				createOrderRequest.getProductId(),
				createOrderRequest.getSize()
			);
			if (productSize == null) {
				throw new BadRequestExp("Size giày sản phẩm tạm hết, Vui lòng chọn sản phẩm khác!");
			}

			// Kiểm tra giá sản phẩm
			if (product.get().getSalePrice() != createOrderRequest.getProductPrice()) {
				throw new BadRequestExp("Giá sản phẩm thay đổi, Vui lòng đặt hàng lại!");
			}

			Order order = new Order();
			User user = new User();
			user.setId(userId);
			order.setCreatedBy(user);
			order.setBuyer(user);

			// Xử lý promotion nếu có
			if (createOrderRequest.getCouponCode() != "") {
				Promotion promotion = promotionService.checkPromotion(createOrderRequest.getCouponCode());
				if (promotion == null) {
					throw new NotFoundExp("Mã khuyến mãi không tồn tại hoặc chưa được kích hoạt");
				}
				
				long promotionPrice = promotionService.calculatePromotionPrice(
					createOrderRequest.getProductPrice(),
					promotion
				);
				
				if (promotionPrice != createOrderRequest.getTotalPrice()) {
					throw new BadRequestExp("Tổng giá trị đơn hàng thay đổi. Vui lòng kiểm tra và đặt lại đơn hàng");
				}
				
				Order.UsedPromotion usedPromotion = new Order.UsedPromotion(
					createOrderRequest.getCouponCode(),
					promotion.getDiscountType(),
					promotion.getDiscountValue(),
					promotion.getMaximumDiscountValue()
				);
				order.setPromotion(usedPromotion);
			}

			// Tạo OrderItem và thêm vào Order
			OrderItem orderItem = new OrderItem();
			orderItem.setProduct(product.get());
			orderItem.setSize(createOrderRequest.getSize());
			orderItem.setQuantity(1);
			orderItem.setPrice(Double.valueOf(createOrderRequest.getProductPrice()));
			orderItem.calculateSubtotal();
			order.addOrderItem(orderItem);

			// Cập nhật thông tin đơn hàng
			order.setCreatedAt(new Timestamp(System.currentTimeMillis()));
			order.setReceiverName(createOrderRequest.getReceiverName());
			order.setReceiverPhone(createOrderRequest.getReceiverPhone());
			order.setNote(createOrderRequest.getNote());
			order.setTotalPrice(BigDecimal.valueOf(createOrderRequest.getTotalPrice()).doubleValue());
			order.setStatus(Contant.ORDER_STATUS);

			// Lưu đơn hàng
			Order savedOrder = orderRepository.save(order);

			// Cập nhật số lượng sản phẩm
			productSizeRepository.minusOneProductBySize(orderItem.getProduct().getId(), orderItem.getSize());
			productRepository.plusOneProductTotalSold(orderItem.getProduct().getId());

			return savedOrder;
		} catch (Exception e) {
			throw new InternalServerExp("Có lỗi khi tạo đơn hàng: " + e.getMessage());
		}
	}

	@Override
	@Transactional
	public Order createOrder(CreateOrderRequest createOrderRequest, long userId) {
		try {
			// Kiểm tra sản phẩm có tồn tại
			Optional<Product> product = productRepository.findById(createOrderRequest.getProductId());
			if (product.isEmpty()) {
				throw new NotFoundExp("Sản phẩm không tồn tại!");
			}

			// Kiểm tra size có sẵn
			ProductSize productSize = productSizeRepository.checkProductAndSizeAvailable(createOrderRequest.getProductId(),
					createOrderRequest.getSize());
			if (productSize == null) {
				throw new BadRequestExp("Size giày sản phẩm tạm hết, Vui lòng chọn sản phẩm khác!");
			}

			// Kiểm tra giá sản phẩm
			if (product.get().getSalePrice() != createOrderRequest.getProductPrice()) {
				throw new BadRequestExp("Giá sản phẩm thay đổi, Vui lòng đặt hàng lại!");
			}
			
			Order order = new Order();
			User user = new User();
			user.setId(userId);
			order.setCreatedBy(user);
			order.setBuyer(user);
			
			// Tạo OrderItem thay vì set trực tiếp vào Order
			OrderItem orderItem = new OrderItem();
			orderItem.setProduct(product.get());
			orderItem.setSize(createOrderRequest.getSize());
			orderItem.setQuantity(1);
			orderItem.setPrice(Double.valueOf(createOrderRequest.getProductPrice()));
			orderItem.calculateSubtotal();
			
			// Thêm item vào order
			order.addOrderItem(orderItem);
			
			// Cập nhật số lượng sản phẩm
			productSizeRepository.minusOneProductBySize(orderItem.getProduct().getId(), orderItem.getSize());
			productRepository.plusOneProductTotalSold(orderItem.getProduct().getId());
			
			if (createOrderRequest.getCouponCode() != "") {
				Promotion promotion = promotionService.checkPromotion(createOrderRequest.getCouponCode());
				if (promotion == null) {
					throw new NotFoundExp("Mã khuyến mãi không tồn tại hoặc chưa được kích hoạt");
				}
				double promotionPrice = promotionService.calculatePromotionPrice(createOrderRequest.getProductPrice(),
						promotion);
				if (Math.round(promotionPrice) != createOrderRequest.getTotalPrice()) {
					throw new BadRequestExp("Tổng giá trị đơn hàng thay đổi. Vui lòng kiểm tra và đặt lại đơn hàng");
				}
				Order.UsedPromotion usedPromotion = new Order.UsedPromotion(createOrderRequest.getCouponCode(),
						promotion.getDiscountType(), promotion.getDiscountValue(), promotion.getMaximumDiscountValue());
				order.setPromotion(usedPromotion);
			}
			
			order.setCreatedAt(new Timestamp(System.currentTimeMillis()));
			order.setReceiverAddress(createOrderRequest.getReceiverAddress());
			order.setReceiverName(createOrderRequest.getReceiverName());
			order.setReceiverPhone(createOrderRequest.getReceiverPhone());
			order.setNote(createOrderRequest.getNote());
			order.setTotalPrice(BigDecimal.valueOf(createOrderRequest.getTotalPrice()).doubleValue());
			order.setStatus(Contant.ORDER_STATUS);

			// Lưu đơn hàng
			Order savedOrder = orderRepository.save(order);
			
			// Cập nhật số lượng sản phẩm
			for (OrderItem item : savedOrder.getItems()) {
				productSizeRepository.minusOneProductBySize(item.getProduct().getId(), item.getSize());
				productRepository.plusOneProductTotalSold(item.getProduct().getId());
			}
			
			// Tạo hóa đơn sau khi đơn hàng được tạo thành công
			CreateInvoiceRequest invoiceRequest = new CreateInvoiceRequest();
			invoiceRequest.setOrderId(savedOrder.getId());
			
			// Tính tổng tiền từ các items
			double subtotal = savedOrder.getItems().stream()
					.mapToDouble(item -> item.getPrice() * item.getQuantity())
					.sum();
			invoiceRequest.setSubtotal(new BigDecimal(subtotal));
			
			// Tính toán discount nếu có
			BigDecimal discount = BigDecimal.ZERO;
			if (savedOrder.getPromotion() != null) {
				if (savedOrder.getPromotion().getDiscountType() == 1) {
					// Giảm giá theo phần trăm
					discount = new BigDecimal(subtotal)
						.multiply(new BigDecimal(savedOrder.getPromotion().getDiscountValue()))
						.divide(new BigDecimal(100));
				} else {
					// Giảm giá trực tiếp
					discount = new BigDecimal(savedOrder.getPromotion().getDiscountValue());
				}
			}
			invoiceRequest.setDiscount(discount);
			
			// Tính thuế (ví dụ: 10% của subtotal)
			BigDecimal tax = new BigDecimal(subtotal)
				.multiply(new BigDecimal("0.1"));
			invoiceRequest.setTax(tax);
			
			// Tính tổng tiền
			double totalAmount = savedOrder.getTotalPrice();
			invoiceRequest.setTotal(BigDecimal.valueOf(totalAmount));
			
			// Mặc định là thanh toán khi nhận hàng
			invoiceRequest.setPaymentMethod("CASH");
			invoiceRequest.setPaymentStatus(0); // Chưa thanh toán
			invoiceRequest.setNote("Đơn hàng mới tạo");
			
			// Tạo hóa đơn
			invoiceService.createInvoice(invoiceRequest);
			
			return savedOrder;
		} catch (Exception e) {
			throw new InternalServerExp("Có lỗi khi tạo đơn hàng: " + e.getMessage());
		}
	}

	@Override
	public void updateDetailOrder(UpdateDetailOrder updateDetailOrder, long id, long userId) {
		try {
			// Kiểm tra có đơn hàng
			Optional<Order> rs = orderRepository.findById(id);
			if (rs.isEmpty()) {
				throw new NotFoundExp("Đơn hàng không tồn tại");
			}

			Order order = rs.get();
			// Kiểm tra trạng thái đơn hàng
			if (order.getStatus() != Contant.ORDER_STATUS) {
				throw new BadRequestExp("Chỉ cập nhật đơn hàng ở trạng thái chờ lấy hàng");
			}

			// Kiểm tra sản phẩm
			Optional<Product> product = productRepository.findById(updateDetailOrder.getProductId());
			if (product.isEmpty()) {
				throw new BadRequestExp("Sản phẩm không tồn tại");
			}

			// Kiểm tra giá
			double price = updateDetailOrder.getProductPrice();
			if (product.get().getSalePrice() != price) {
				throw new BadRequestExp("Giá sản phẩm thay đổi vui lòng đặt hàng lại");
			}

			// Kiểm tra size có sẵn
			ProductSize productSize = productSizeRepository.checkProductAndSizeAvailable(
				updateDetailOrder.getProductId(),
				updateDetailOrder.getSize()
			);
			if (productSize == null) {
				throw new BadRequestExp("Size giày sản phẩm tạm hết, Vui lòng chọn sản phẩm khác");
			}

			// Xóa các item cũ
			order.getItems().clear();
			
			// Tạo và thêm item mới
			OrderItem orderItem = new OrderItem();
			orderItem.setProduct(product.get());
			orderItem.setSize(updateDetailOrder.getSize());
			orderItem.setQuantity(1); // quantity mặc định là 1
			// Chuyển đổi giá từ int sang Double một cách an toàn
			orderItem.setPrice(Double.valueOf(price));
			orderItem.calculateSubtotal();
			order.addOrderItem(orderItem);
			
			// Kiểm tra và cập nhật khuyến mãi
			if (updateDetailOrder.getCouponCode() != "") {
				Promotion promotion = promotionService.checkPromotion(updateDetailOrder.getCouponCode());
				if (promotion == null) {
					throw new NotFoundExp("Mã khuyến mãi không tồn tại hoặc chưa được kích hoạt");
				}
				long promotionPrice = promotionService.calculatePromotionPrice(
                        (long) price,
					promotion
				);
				// Chuyển đổi rõ ràng từ double sang long bằng cách làm tròn
				long roundedPromotionPrice = Math.round(promotionPrice);

				// So sánh với totalPrice
				if (roundedPromotionPrice != updateDetailOrder.getTotalPrice()) {
					throw new BadRequestExp("Tổng giá trị đơn hàng thay đổi. Vui lòng kiểm tra và đặt lại đơn hàng");
				}
				Order.UsedPromotion usedPromotion = new Order.UsedPromotion(
					updateDetailOrder.getCouponCode(),
					promotion.getDiscountType(),
					promotion.getDiscountValue(),
					promotion.getMaximumDiscountValue()
				);
				order.setPromotion(usedPromotion);
			}

			// Cập nhật thông tin đơn hàng
			order.setModifiedAt(new Timestamp(System.currentTimeMillis()));
			order.calculateTotalPrice(); // Sử dụng phương thức mới để tính tổng tiền
			order.setStatus(Contant.ORDER_STATUS);
			
			User user = new User();
			user.setId(userId);
			order.setModifiedBy(user);

			orderRepository.save(order);
		} catch (Exception e) {
			throw new InternalServerExp("Lỗi khi cập nhật");
		}
	}

	@Override
	public Order findOrderById(long id) {
		Optional<Order> orderOpt = orderRepository.findById(id);
		if (orderOpt.isEmpty()) {
			throw new NotFoundExp("Đơn hàng không tồn tại");
		}
		Order order = orderOpt.get();
		
		// Đảm bảo danh sách items và products được tải đầy đủ
		if (order.getItems() != null) {
			order.getItems().forEach(item -> {
				// Force load product data
				if (item.getProduct() != null) {
					item.getProduct().getName(); // Truy cập để load dữ liệu
				}
			});
		}
		
		return order;
	}

	@Override
	@Transactional
	public void updateStatusOrder(UpdateStatusOrderRequest updateStatusOrderRequest, long orderId, long userId) {
		try {
			Order order = findOrderById(orderId);
			
			// Cập nhật trạng thái đơn hàng
			order.setStatus(updateStatusOrderRequest.getStatus());
			orderRepository.save(order);
			
			// Cập nhật trạng thái thanh toán trong hóa đơn nếu đơn hàng đã hoàn thành
			if (updateStatusOrderRequest.getStatus() == Contant.COMPLETED_STATUS) {
				Invoice invoice = invoiceService.findByOrderId(orderId);
				if (invoice != null) {
					CreateInvoiceRequest updateInvoice = new CreateInvoiceRequest();
					updateInvoice.setPaymentStatus(1); // Đã thanh toán
					invoiceService.updateInvoice(updateInvoice, invoice.getId());
				}
			}
			
		} catch (Exception e) {
			throw new InternalServerExp("Có lỗi khi cập nhật trạng thái đơn hàng: " + e.getMessage());
		}
	}

	@Override
	public List<OrderInfoDTO> getListOrderOfPersonByStatus(int status, long userId) {
		List<Order> orders;
		try {
			if (status == 0) {
				orders = orderRepository.findByBuyerIdOrderByCreatedAtDesc(userId);
			} else {
				orders = orderRepository.findByBuyerIdAndStatusOrderByCreatedAtDesc(userId, status);
			}

			// Tạo một map để nhóm OrderInfoDTO theo ID đơn hàng
			Map<Long, OrderInfoDTO> orderMap = new HashMap<>();

			for (Order order : orders) {
				// Tạo hoặc lấy OrderInfoDTO từ map
				OrderInfoDTO dto = orderMap.getOrDefault(order.getId(), new OrderInfoDTO());
				
				if (!orderMap.containsKey(order.getId())) {
					// Thông tin cơ bản của đơn hàng 
					dto.setId(order.getId());
					dto.setStatus(order.getStatus());
					dto.setTotalPrice(BigDecimal.valueOf(order.getTotalPrice()));
					dto.setCreatedAt(order.getCreatedAt().toString());
					dto.setTotalItems(order.getItems().size());
					
					// Thông tin sản phẩm đầu tiên (giữ để tương thích ngược)
					if (!order.getItems().isEmpty()) {
						OrderItem firstItem = order.getItems().get(0);
						Product product = firstItem.getProduct();
						if (product != null && !product.getImages().isEmpty()) {
							dto.setProductImg(product.getImages().get(0));
						}
						dto.setProductName(firstItem.getProduct().getName());
						dto.setSizeVn(firstItem.getSize());
					}
					
					// Thêm vào map
					orderMap.put(order.getId(), dto);
				}
			}
			
			return new ArrayList<>(orderMap.values());
		} catch (Exception e) {
			throw new InternalServerExp("Lỗi khi lấy danh sách đơn hàng: " + e.getMessage());
		}
	}

	@Override
	public OrderDetailDTO userGetDetailById(long id, long userId) {
		// Lấy thông tin đơn hàng
		Order order = orderRepository.findById(id)
				.orElseThrow(() -> new NotFoundExp("Không tìm thấy đơn hàng"));
		
		// Kiểm tra người dùng có quyền xem đơn hàng không
		if (order.getBuyer().getId() != userId) {
			throw new AccessDeniedException("Bạn không có quyền xem đơn hàng này");
		}
		
		// Tạo DTO chứa thông tin đơn hàng
		OrderDetailDTO orderDetailDTO = new OrderDetailDTO();
		orderDetailDTO.setId(order.getId());
		orderDetailDTO.setStatus(order.getStatus());
		orderDetailDTO.setTotalPrice(BigDecimal.valueOf(order.getTotalPrice()));
		
		// Tính tổng giá sản phẩm (không bao gồm khuyến mãi)
		double productPrice = 0;
		for (OrderItem item : order.getItems()) {
			productPrice += item.getSubtotal();
		}
		orderDetailDTO.setProductPrice(BigDecimal.valueOf(productPrice));
		
		orderDetailDTO.setCreatedAt(order.getCreatedAt().toString());
		orderDetailDTO.setReceiverName(order.getReceiverName());
		orderDetailDTO.setReceiverPhone(order.getReceiverPhone());
		orderDetailDTO.setReceiverAddress(order.getReceiverAddress());
		
		// Thiết lập trạng thái đơn hàng dưới dạng văn bản
		switch (order.getStatus()) {
			case 1:
				orderDetailDTO.setStatusText("Chờ lấy hàng");
				break;
			case 2:
				orderDetailDTO.setStatusText("Đang giao hàng");
				break;
			case 3:
				orderDetailDTO.setStatusText("Đã giao hàng");
				break;
			case 4:
				orderDetailDTO.setStatusText("Đã trả lại");
				break;
			case 5:
				orderDetailDTO.setStatusText("Đã hủy");
				break;
			default:
				orderDetailDTO.setStatusText("Không xác định");
		}
		
		// Thêm thông tin về tất cả các mặt hàng trong đơn hàng
		if (!order.getItems().isEmpty()) {
			// Lưu sản phẩm đầu tiên vào các trường cũ (để tương thích ngược)
			OrderItem firstItem = order.getItems().get(0);
			orderDetailDTO.setProductName(firstItem.getProduct().getName());
			orderDetailDTO.setSizeVn(firstItem.getSize());
			if (!firstItem.getProduct().getImages().isEmpty()) {
				orderDetailDTO.setProductImg(firstItem.getProduct().getImages().get(0));
			}
			
			// Lưu tất cả các sản phẩm vào danh sách items
			for (OrderItem item : order.getItems()) {
				OrderItemDTO itemDTO = new OrderItemDTO();
				itemDTO.setId(item.getId());
				itemDTO.setProductId(item.getProduct().getId());
				itemDTO.setProductName(item.getProduct().getName());
				if (!item.getProduct().getImages().isEmpty()) {
					itemDTO.setProductImg(item.getProduct().getImages().get(0));
				}
				itemDTO.setQuantity(item.getQuantity());
				itemDTO.setSizeVn(item.getSize());
				itemDTO.setPrice(BigDecimal.valueOf(item.getPrice()));
				itemDTO.setSubtotal(BigDecimal.valueOf(item.getSubtotal()));
				
				orderDetailDTO.getItems().add(itemDTO);
			}
		}
		
		return orderDetailDTO;
	}

	@Override
	public void userCancelOrder(long id, long userId) {
		Optional<Order> rs = orderRepository.findById(id);
		if (rs.isEmpty()) {
			throw new NotFoundExp("Đơn hàng không tồn tại");
		}
		Order order = rs.get();
		if (order.getBuyer().getId() != userId) {
			throw new BadRequestExp("Bạn không phải chủ nhân đơn hàng");
		}
		if (order.getStatus() != Contant.ORDER_STATUS) {
			throw new BadRequestExp(
					"Trạng thái đơn hàng không phù hợp để hủy. Vui lòng liên hệ với shop để được hỗ trợ");
		}

		order.setStatus(Contant.CANCELED_STATUS);
		orderRepository.save(order);
	}

	@Override
	public long getCountOrder() {
		return orderRepository.count();
	}

	public void statistic(long amount, int quantity, Order order) {
		try {
			Statistic statistic = statisticRepository.findByCreatedAT();
			if (statistic != null) {
				statistic.setOrder(order);
				statistic.setSales(statistic.getSales() + amount);
				statistic.setQuantity(statistic.getQuantity() + quantity);
				
				// Tính toán chi phí sản phẩm
				long totalCost = 0L;
				for (OrderItem item : order.getItems()) {
					// Chuyển đổi giá thành long một cách an toàn
					long itemPrice = Math.round(item.getProduct().getPrice());
					totalCost += itemPrice * item.getQuantity();
				}
				
				// Tính lợi nhuận
				statistic.setProfit(statistic.getSales() - totalCost);
				
				statisticRepository.save(statistic);
			}
		} catch (Exception e) {
			throw new InternalServerExp("Lỗi khi cập nhật thống kê");
		}
	}

	public void updateStatistic(long amount, int quantity, Order order) {
		Statistic statistic = statisticRepository.findByCreatedAT();
		if (statistic != null) {
			statistic.setOrder(order);
			statistic.setSales(statistic.getSales() - amount);
			statistic.setQuantity(statistic.getQuantity() - quantity);
			
			// Tính toán chi phí sản phẩm và chuyển đổi thành long
			long totalCost = 0L;
			for (OrderItem item : order.getItems()) {
				long itemPrice = Math.round(item.getProduct().getPrice());
				totalCost += itemPrice * item.getQuantity();
			}
			
			// Tính lợi nhuận
			statistic.setProfit(statistic.getSales() - totalCost);
			
			statisticRepository.save(statistic);
		}
	}

	@Override
	public void userReturnOrder(long id, long userId, UpdateStatusOrderRequest updateStatusOrderRequest) {
		Optional<Order> rs = orderRepository.findById(id);
		if (rs.isEmpty()) {
			throw new NotFoundExp("Đơn hàng không tồn tại");
		}
		Order order = rs.get();
		if (order.getBuyer().getId() != userId) {
			throw new BadRequestExp("Bạn không phải chủ nhân đơn hàng");
		}
		if (order.getStatus() != Contant.COMPLETED_STATUS) {
			throw new BadRequestExp(
					"Trạng thái đơn hàng không phù hợp để hủy. Vui lòng liên hệ với shop để được hỗ trợ");
		}
		order.setNote(updateStatusOrderRequest.getNote());
		order.setStatus(Contant.RETURNED_STATUS);
		orderRepository.save(order);
	}

	@Override
	@Transactional
	public Order createOrderFromCart(Long userId, CheckoutRequestDTO request) {
		User buyer = userRepository.findById(userId)
			.orElseThrow(() -> new NotFoundExp("User not found"));

		Order order = new Order();
		order.setBuyer(buyer);
		order.setReceiverName(request.getReceiverName());
		order.setReceiverPhone(request.getReceiverPhone());
		order.setReceiverAddress(request.getReceiverAddress());
		order.setStatus(Contant.ORDER_STATUS);
		order.setCreatedAt(new Timestamp(System.currentTimeMillis()));
		order.setCreatedBy(buyer);

		// Lấy giỏ hàng
		Cart cart = cartService.getCartByUserId(userId);
		if (cart == null || cart.getItems().isEmpty()) {
			throw new BadRequestExp("Giỏ hàng trống");
		}

		// Log để debug
		System.out.println("Creating order from cart with " + cart.getItems().size() + " items");
		
		// Tạo order items từ cart items
		if (order.getItems() == null) {
			order.setItems(new ArrayList<>());
		}
		
		for (CartItem cartItem : cart.getItems()) {
			OrderItem orderItem = new OrderItem();
			orderItem.setProduct(cartItem.getProduct());
			orderItem.setSize(cartItem.getSize());
			orderItem.setQuantity(cartItem.getQuantity());
			orderItem.setPrice(cartItem.getPrice());
			orderItem.calculateSubtotal();
			orderItem.setOrder(order); // Set mối quan hệ với order
			order.getItems().add(orderItem);
			
			// Cập nhật số lượng sản phẩm trong kho
			productSizeRepository.minusOneProductBySize(
				cartItem.getProduct().getId(), 
				cartItem.getSize()
			);
		}

		// Xử lý mã giảm giá nếu có
		if (request.getCouponCode() != null && !request.getCouponCode().isEmpty()) {
			Promotion promotion = promotionService.checkPromotion(request.getCouponCode());
			if (promotion != null) {
				Order.UsedPromotion usedPromotion = new Order.UsedPromotion(
					request.getCouponCode(),
					promotion.getDiscountType(),
					promotion.getDiscountValue(),
					promotion.getMaximumDiscountValue()
				);
				order.setPromotion(usedPromotion);
			}
		}

		// Tính tổng tiền đơn hàng
		order.calculateTotalPrice();

		// Lưu đơn hàng
		Order savedOrder = orderRepository.save(order);
		
		// Xóa giỏ hàng sau khi đặt hàng thành công
		cartService.clearCart(userId);

		return savedOrder;
	}

	// Helper method - có thể giữ lại để sử dụng trong các phương thức khác
	private void addOrderItem(Order order, OrderItem item) {
		if (order.getItems() == null) {
			order.setItems(new ArrayList<>());
		}
		order.getItems().add(item);
		item.setOrder(order); // Đảm bảo thiết lập mối quan hệ hai chiều
	}

	@Override
	public List<Order> getAllOrders() {
		return orderRepository.findAll();
	}

	@Override
	public long countOrdersByStatus(int status) {
		return orderRepository.countByStatus(status);
	}
}
