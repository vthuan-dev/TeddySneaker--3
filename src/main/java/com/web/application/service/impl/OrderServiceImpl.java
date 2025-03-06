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
		if (createOrderRequest.getCouponCode() != "") {
			Promotion promotion = promotionService.checkPromotion(createOrderRequest.getCouponCode());
			if (promotion == null) {
				throw new NotFoundExp("Mã khuyến mãi không tồn tại hoặc chưa được kích hoạt");
			}
			long promotionPrice = promotionService.calculatePromotionPrice(createOrderRequest.getProductPrice(),
					promotion);
			if (promotionPrice != createOrderRequest.getTotalPrice()) {
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
		order.setSize(createOrderRequest.getSize());
		order.setPrice(createOrderRequest.getProductPrice());
		order.setTotalPrice(createOrderRequest.getTotalPrice());
		order.setStatus(Contant.COUNTER_STATUS);
		order.setQuantity(1);
		order.setProduct(product.get());
		orderRepository.save(order);
		productSizeRepository.minusOneProductBySize(order.getProduct().getId(), order.getSize());
		productRepository.plusOneProductTotalSold(order.getProduct().getId());
		return order;

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
			if (createOrderRequest.getCouponCode() != "") {
				Promotion promotion = promotionService.checkPromotion(createOrderRequest.getCouponCode());
				if (promotion == null) {
					throw new NotFoundExp("Mã khuyến mãi không tồn tại hoặc chưa được kích hoạt");
				}
				long promotionPrice = promotionService.calculatePromotionPrice(createOrderRequest.getProductPrice(),
						promotion);
				if (promotionPrice != createOrderRequest.getTotalPrice()) {
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
			order.setSize(createOrderRequest.getSize());
			order.setPrice(createOrderRequest.getProductPrice());
			order.setTotalPrice(createOrderRequest.getTotalPrice());
			order.setStatus(Contant.ORDER_STATUS);
			order.setQuantity(1);
			order.setProduct(product.get());

			// Lưu đơn hàng
			Order savedOrder = orderRepository.save(order);
			
			// Tạo hóa đơn sau khi đơn hàng được tạo thành công
			CreateInvoiceRequest invoiceRequest = new CreateInvoiceRequest();
			invoiceRequest.setOrderId(savedOrder.getId());
			invoiceRequest.setSubtotal(new BigDecimal(savedOrder.getPrice()));
			
			// Tính toán discount nếu có
			BigDecimal discount = BigDecimal.ZERO;
			if (savedOrder.getPromotion() != null) {
				if (savedOrder.getPromotion().getDiscountType() == 1) {
					// Giảm giá theo phần trăm
					discount = new BigDecimal(savedOrder.getPrice())
						.multiply(new BigDecimal(savedOrder.getPromotion().getDiscountValue()))
						.divide(new BigDecimal(100));
				} else {
					// Giảm giá trực tiếp
					discount = new BigDecimal(savedOrder.getPromotion().getDiscountValue());
				}
			}
			invoiceRequest.setDiscount(discount);
			
			// Tính thuế (ví dụ: 10% của subtotal)
			BigDecimal tax = new BigDecimal(savedOrder.getPrice())
				.multiply(new BigDecimal("0.1"));
			invoiceRequest.setTax(tax);
			
			// Tính tổng tiền
			BigDecimal total = new BigDecimal(savedOrder.getTotalPrice());
			invoiceRequest.setTotal(total);
			
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
		// Kiểm trả có đơn hàng
		Optional<Order> rs = orderRepository.findById(id);
		if (rs.isEmpty()) {
			throw new NotFoundExp("Đơn hàng không tồn tại");
		}

		Order order = rs.get();
		// Kiểm tra trạng thái đơn hàng
		if (order.getStatus() != Contant.ORDER_STATUS) {
			throw new BadRequestExp("Chỉ cập nhật đơn hàng ở trạng thái chờ lấy hàng");
		}

		// Kiểm tra size sản phẩm
		Optional<Product> product = productRepository.findById(updateDetailOrder.getProductId());
		if (product.isEmpty()) {
			throw new BadRequestExp("Sản phẩm không tồn tại");
		}
		// Kiểm tra giá
		if (product.get().getSalePrice() != updateDetailOrder.getProductPrice()) {
			throw new BadRequestExp("Giá sản phẩm thay đổi vui lòng đặt hàng lại");
		}

		ProductSize productSize = productSizeRepository.checkProductAndSizeAvailable(updateDetailOrder.getProductId(),
				updateDetailOrder.getSize());
		if (productSize == null) {
			throw new BadRequestExp("Size giày sản phẩm tạm hết, Vui lòng chọn sản phẩm khác");
		}

		// Kiểm tra khuyến mại
		if (updateDetailOrder.getCouponCode() != "") {
			Promotion promotion = promotionService.checkPromotion(updateDetailOrder.getCouponCode());
			if (promotion == null) {
				throw new NotFoundExp("Mã khuyến mãi không tồn tại hoặc chưa được kích hoạt");
			}
			long promotionPrice = promotionService.calculatePromotionPrice(updateDetailOrder.getProductPrice(),
					promotion);
			if (promotionPrice != updateDetailOrder.getTotalPrice()) {
				throw new BadRequestExp("Tổng giá trị đơn hàng thay đổi. Vui lòng kiểm tra và đặt lại đơn hàng");
			}
			Order.UsedPromotion usedPromotion = new Order.UsedPromotion(updateDetailOrder.getCouponCode(),
					promotion.getDiscountType(), promotion.getDiscountValue(), promotion.getMaximumDiscountValue());
			order.setPromotion(usedPromotion);
		}

		order.setModifiedAt(new Timestamp(System.currentTimeMillis()));
		order.setProduct(product.get());
		order.setSize(updateDetailOrder.getSize());
		order.setPrice(updateDetailOrder.getProductPrice());
		order.setTotalPrice(updateDetailOrder.getTotalPrice());

		order.setStatus(Contant.ORDER_STATUS);
		User user = new User();
		user.setId(userId);
		order.setModifiedBy(user);
		try {
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
			if (updateStatusOrderRequest.getStatus() == Contant.ORDER_STATUS_DONE) {
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
		Statistic statistic = statisticRepository.findByCreatedAT();
		if (statistic != null) {
			statistic.setOrder(order);
			statistic.setSales(statistic.getSales() + amount);
			statistic.setQuantity(statistic.getQuantity() + quantity);
			statistic.setProfit(statistic.getSales() - (statistic.getQuantity() * order.getProduct().getPrice()));
			statisticRepository.save(statistic);
		} else {
			Statistic statistic1 = new Statistic();
			statistic1.setOrder(order);
			statistic1.setSales(amount);
			statistic1.setQuantity(quantity);
			statistic1.setProfit(amount - (quantity * order.getProduct().getPrice()));
			statistic1.setCreatedAt(new Timestamp(System.currentTimeMillis()));
			statisticRepository.save(statistic1);
		}
	}

	public void updateStatistic(long amount, int quantity, Order order) {
		Statistic statistic = statisticRepository.findByCreatedAT();
		if (statistic != null) {
			statistic.setOrder(order);
			statistic.setSales(statistic.getSales() - amount);
			statistic.setQuantity(statistic.getQuantity() - quantity);
			statistic.setProfit(statistic.getSales() - (statistic.getQuantity() * order.getProduct().getPrice()));
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
}
