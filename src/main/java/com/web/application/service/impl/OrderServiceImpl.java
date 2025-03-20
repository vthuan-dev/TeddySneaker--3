package com.web.application.service.impl;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

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
import com.web.application.dto.CheckoutRequestDTO;

@Component
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

	@Override
	public Page<Order> adminGetListOrders(String id, String name, String phone, String status, String product,
			String createdAt, String modifiedAt, int page) {
		page--;
		if (page < 0) {
			page = 0;
		}
		int limit = 10;
		Pageable pageable = PageRequest.of(page, limit, Sort.by("id").ascending());
		return orderRepository.adminGetListOrder(id, name, phone, status, product, createdAt, modifiedAt, pageable);
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
			order.addItem(orderItem);

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
			order.addItem(orderItem);
			
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
			order.addItem(orderItem);
			
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
		Optional<Order> order = orderRepository.findById(id);
		if (order.isEmpty()) {
			throw new NotFoundExp("Đơn hàng không tồn tại");
		}
		return order.get();
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
		List<OrderInfoDTO> list = orderRepository.getListOrderOfPersonByStatus(status, userId);
		return list;
	}

	@Override
	public OrderDetailDTO userGetDetailById(long id, long userId) {
		OrderDetailDTO order = orderRepository.userGetDetailById(id, userId);
		if (order == null) {
			return null;
		}

		if (order.getStatus() == Contant.ORDER_STATUS) {
			order.setStatusText("Chờ lấy hàng");
		} else if (order.getStatus() == Contant.DELIVERY_STATUS) {
			order.setStatusText("Đang giao hàng");
		} else if (order.getStatus() == Contant.COMPLETED_STATUS) {
			order.setStatusText("Đã giao hàng");
		} else if (order.getStatus() == Contant.CANCELED_STATUS) {
			order.setStatusText("Đơn hàng đã trả lại");
		} else if (order.getStatus() == Contant.RETURNED_STATUS) {
			order.setStatusText("Đơn hàng đã hủy");
		}

		return order;
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
		try {
			// Lấy giỏ hàng của người dùng
			List<CartItem> cartItems = cartService.findByUserId(userId);
			if (cartItems.isEmpty()) {
				throw new BadRequestExp("Giỏ hàng trống, không thể tạo đơn hàng");
			}
			
			// Tạo đơn hàng mới
			Order order = new Order();
			User user = new User();
			user.setId(userId);
			order.setCreatedBy(user);
			order.setBuyer(user);
			
			// Thêm từng sản phẩm trong giỏ hàng vào đơn hàng
			for (CartItem cartItem : cartItems) {
				// Kiểm tra sản phẩm và size có sẵn
				ProductSize productSize = productSizeRepository.checkProductAndSizeAvailable(
					cartItem.getProduct().getId(), cartItem.getSize());
				if (productSize == null || productSize.getQuantity() < cartItem.getQuantity()) {
					throw new BadRequestExp("Sản phẩm " + cartItem.getProduct().getName() + 
						" size " + cartItem.getSize() + " không đủ số lượng");
				}
				
				// Tạo OrderItem từ CartItem
				OrderItem orderItem = new OrderItem();
				orderItem.setProduct(cartItem.getProduct());
				orderItem.setSize(cartItem.getSize());
				orderItem.setQuantity(cartItem.getQuantity());
				orderItem.setPrice(Double.valueOf(cartItem.getProduct().getSalePrice()));
				orderItem.calculateSubtotal();
				
				// Thêm vào đơn hàng
				order.addItem(orderItem);
			}
			
			// Kiểm tra và áp dụng mã giảm giá nếu có
			if (request.getCouponCode() != null && !request.getCouponCode().isEmpty()) {
				Promotion promotion = promotionService.checkPromotion(request.getCouponCode());
				if (promotion == null) {
					throw new NotFoundExp("Mã khuyến mãi không tồn tại hoặc chưa được kích hoạt");
				}
				
				// Tính tổng giá trị đơn hàng trước khi áp dụng khuyến mãi
				double subtotal = order.getItems().stream()
					.mapToDouble(item -> item.getPrice() * item.getQuantity())
					.sum();
				
				// Tính giá sau khuyến mãi
				double discountAmount = 0;
				if (promotion.getDiscountType() == 1) {
					// Giảm theo phần trăm
					discountAmount = subtotal * promotion.getDiscountValue() / 100;
					// Kiểm tra giới hạn giảm giá tối đa
					if (promotion.getMaximumDiscountValue() > 0 && 
						discountAmount > promotion.getMaximumDiscountValue()) {
						discountAmount = promotion.getMaximumDiscountValue();
					}
				} else {
					// Giảm trực tiếp
					discountAmount = promotion.getDiscountValue();
				}
				
				// Tính tổng sau khuyến mãi
				double totalAfterDiscount = subtotal - discountAmount;
				
				// Lưu thông tin khuyến mãi vào đơn hàng
				Order.UsedPromotion usedPromotion = new Order.UsedPromotion(
					request.getCouponCode(),
					promotion.getDiscountType(),
					promotion.getDiscountValue(),
					promotion.getMaximumDiscountValue()
				);
				order.setPromotion(usedPromotion);
				
				// Cập nhật tổng tiền
				order.setTotalPrice(totalAfterDiscount);
			} else {
				// Nếu không có mã giảm giá, tổng tiền là tổng của các items
				double total = order.getItems().stream()
					.mapToDouble(item -> item.getPrice() * item.getQuantity())
					.sum();
				order.setTotalPrice(total);
			}
			
			// Cập nhật thông tin giao hàng
			order.setCreatedAt(new Timestamp(System.currentTimeMillis()));
			order.setReceiverAddress(request.getReceiverAddress());
			order.setReceiverName(request.getReceiverName());
			order.setReceiverPhone(request.getReceiverPhone());
			order.setNote(request.getNote());
			order.setStatus(Contant.ORDER_STATUS);
			
			// Lưu đơn hàng
			Order savedOrder = orderRepository.save(order);
			
			// Cập nhật số lượng sản phẩm
			for (OrderItem item : savedOrder.getItems()) {
				productSizeRepository.minusOneProductBySize(item.getProduct().getId(), item.getSize());
				productRepository.plusOneProductTotalSold(item.getProduct().getId());
			}
			
			// Tạo hóa đơn
			CreateInvoiceRequest invoiceRequest = new CreateInvoiceRequest();
			invoiceRequest.setOrderId(savedOrder.getId());
			
			// Chuyển đổi tổng tiền sang BigDecimal
			BigDecimal totalAmount = BigDecimal.valueOf(savedOrder.getTotalPrice());
			invoiceRequest.setTotal(totalAmount);
			
			// Tính toán discount nếu có
			BigDecimal discount = BigDecimal.ZERO;
			if (savedOrder.getPromotion() != null) {
				if (savedOrder.getPromotion().getDiscountType() == 1) {
					// Giảm giá theo phần trăm
					discount = totalAmount
						.multiply(new BigDecimal(savedOrder.getPromotion().getDiscountValue()))
						.divide(new BigDecimal(100));
				} else {
					// Giảm giá trực tiếp
					discount = new BigDecimal(savedOrder.getPromotion().getDiscountValue());
				}
			}
			invoiceRequest.setDiscount(discount);
			
			// Tính thuế (ví dụ: 10% của subtotal)
			BigDecimal tax = totalAmount
				.multiply(new BigDecimal("0.1"));
			invoiceRequest.setTax(tax);
			
			// Mặc định là thanh toán khi nhận hàng
			invoiceRequest.setPaymentMethod("CASH");
			invoiceRequest.setPaymentStatus(0); // Chưa thanh toán
			invoiceRequest.setNote("Đơn hàng mới tạo");
			
			// Tạo hóa đơn
			invoiceService.createInvoice(invoiceRequest);
			
			return savedOrder;
		} catch (Exception e) {
			throw new InternalServerExp("Có lỗi khi tạo đơn hàng từ giỏ hàng: " + e.getMessage());
		}
	}
}
