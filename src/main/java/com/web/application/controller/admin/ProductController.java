package com.web.application.controller.admin;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.List;
import java.util.stream.Collectors;

import javax.validation.Valid;

import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import com.web.application.config.Contant;
import com.web.application.entity.Brand;
import com.web.application.entity.Category;
import com.web.application.entity.Product;
import com.web.application.entity.ProductSize;
import com.web.application.entity.User;
import com.web.application.model.request.CreateProductRequest;
import com.web.application.model.request.CreateSizeCountRequest;
import com.web.application.model.request.UpdateFeedBackRequest;
import com.web.application.repository.ProductSizeRepository;
import com.web.application.security.CustomUserDetails;
import com.web.application.service.BrandService;
import com.web.application.service.CategoryService;
import com.web.application.service.ImageService;
import com.web.application.service.ProductService;

import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
public class ProductController {

	private String xlsx = ".xlsx";
	private static final int BUFFER_SIZE = 4096;
	private static final String TEMP_EXPORT_DATA_DIRECTORY = "\\resources\\reports";
	private static final String EXPORT_DATA_REPORT_FILE_NAME = "San_pham";

	@Autowired
	private ServletContext context;

	@Autowired
	private ProductService productService;
	@Autowired
	ProductSizeRepository productSizeRepository;

	@Autowired
	private BrandService brandService;

	@Autowired
	private CategoryService categoryService;

	@Autowired
	private ImageService imageService;

	@GetMapping("/admin/products")
	public String homePages(Model model, @RequestParam(defaultValue = "", required = false) String id,
			@RequestParam(defaultValue = "", required = false) String name,
			@RequestParam(defaultValue = "", required = false) String category,
			@RequestParam(defaultValue = "", required = false) String brand,
			@RequestParam(defaultValue = "1", required = false) Integer page) {

		// Lấy danh sách nhãn hiệu
		List<Brand> brands = brandService.getListBrand();
		model.addAttribute("brands", brands);
		// Lấy danh sách danh mục
		List<Category> categories = categoryService.getListCategories();
		model.addAttribute("categories", categories);
		// Lấy danh sách sản phẩm
		Page<Product> products = productService.adminGetListProduct(id, name, category, brand, page);
		products.getContent().stream().peek(product -> {
			long totalQuantity = productSizeRepository.getTotalQuantityByProductId(product.getId());
			product.setTotalQuantity(totalQuantity);
		}).collect(Collectors.toList());
		model.addAttribute("products", products.getContent());
		model.addAttribute("totalPages", products.getTotalPages());
		model.addAttribute("currentPage", products.getPageable().getPageNumber() + 1);

		return "admin/product/list";
	}

	@GetMapping("/admin/products/create")
	public String getProductCreatePage(Model model) {
		// Lấy danh sách anh của user
		User user = ((CustomUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal())
				.getUser();
		List<String> images = imageService.getListImageOfUser(user.getId());
		model.addAttribute("images", images);

		// Lấy danh sách nhãn hiệu
		List<Brand> brands = brandService.getListBrand();
		model.addAttribute("brands", brands);
		// Lấy danh sách danh mục
		List<Category> categories = categoryService.getListCategories();
		model.addAttribute("categories", categories);

		return "admin/product/create";
	}

	@GetMapping("/admin/products/{slug}/{id}")
	public String getProductUpdatePage(Model model, @PathVariable String id) {

		// Lấy thông tin sản phẩm theo id
		Product product = productService.getProductById(id);
		model.addAttribute("product", product);

		// Lấy danh sách ảnh của user
		User user = ((CustomUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal())
				.getUser();
		List<String> images = imageService.getListImageOfUser(user.getId());
		model.addAttribute("images", images);

		// Lấy danh sách danh mục
		List<Category> categories = categoryService.getListCategories();
		model.addAttribute("categories", categories);

		// Lấy danh sách nhãn hiệu
		List<Brand> brands = brandService.getListBrand();
		model.addAttribute("brands", brands);

		// Lấy danh sách size
		model.addAttribute("sizeVN", Contant.SIZE_VN);

		// Lấy size của sản phẩm
		List<ProductSize> productSizes = productService.getListSizeOfProduct(id);
		model.addAttribute("productSizes", productSizes);

		return "admin/product/edit";
	}

	@GetMapping("/api/admin/products")
	public ResponseEntity<Object> getListProducts(@RequestParam(defaultValue = "", required = false) String id,
			@RequestParam(defaultValue = "", required = false) String name,
			@RequestParam(defaultValue = "", required = false) String category,
			@RequestParam(defaultValue = "", required = false) String brand,
			@RequestParam(defaultValue = "1", required = false) Integer page) {
		Page<Product> products = productService.adminGetListProduct(id, name, category, brand, page);
		return ResponseEntity.ok(products);
	}

	@GetMapping("/api/admin/products/{id}")
	public ResponseEntity<Object> getProductDetail(@PathVariable String id) {
		Product rs = productService.getProductById(id);
		return ResponseEntity.ok(rs);
	}

	@PostMapping("/api/admin/products")
	public ResponseEntity<Object> createProduct(@Valid @RequestBody CreateProductRequest createProductRequest) {
		Product product = productService.createProduct(createProductRequest);
		return ResponseEntity.ok(product);
	}

	@PutMapping("/api/admin/products/{id}")
	public ResponseEntity<Object> updateProduct(@Valid @RequestBody CreateProductRequest createProductRequest,
			@PathVariable String id) {
		productService.updateProduct(createProductRequest, id);
		return ResponseEntity.ok("Sửa sản phẩm thành công!");
	}

	@DeleteMapping("/api/admin/products")
	public ResponseEntity<Object> deleteProduct(@RequestBody String[] ids) {
		productService.deleteProduct(ids);
		return ResponseEntity.ok("Xóa sản phẩm thành công!");
	}

	@DeleteMapping("/api/admin/products/{id}")
	public ResponseEntity<Object> deleteProductById(@PathVariable String id) {
		productService.deleteProductById(id);
		return ResponseEntity.ok("Xóa sản phẩm thành công!");
	}

	@PutMapping("/api/admin/products/sizes")
	public ResponseEntity<?> updateSizeCount(@Valid @RequestBody CreateSizeCountRequest createSizeCountRequest) {
		productService.createSizeCount(createSizeCountRequest);

		return ResponseEntity.ok("Cập nhật thành công!");
	}

	@PutMapping("/api/admin/products/{id}/update-feedback-image")
	public ResponseEntity<?> updatefeedBackImages(@PathVariable String id,
			@Valid @RequestBody UpdateFeedBackRequest req) {
		productService.updatefeedBackImages(id, req);

		return ResponseEntity.ok("Cập nhật thành công");
	}

	@GetMapping("/api/products/export/excel")
	public void exportProductDataToExcelFile(HttpServletResponse response) {
		List<Product> result = productService.getAllProduct();
		String fullPath = this.generateProductExcel(result, context, EXPORT_DATA_REPORT_FILE_NAME);
		if (fullPath != null) {
			this.fileDownload(fullPath, response, EXPORT_DATA_REPORT_FILE_NAME, "xlsx");
		}
	}

	private String generateProductExcel(List<Product> products, ServletContext context, String fileName) {
		String filePath = context.getRealPath(TEMP_EXPORT_DATA_DIRECTORY);
		File file = new File(filePath);
		if (!file.exists()) {
			new File(filePath).mkdirs();
		}
		try (FileOutputStream fos = new FileOutputStream(file + "\\" + fileName + xlsx);
				XSSFWorkbook workbook = new XSSFWorkbook();) {

			XSSFSheet worksheet = workbook.createSheet("Product");
			worksheet.setDefaultColumnWidth(20);

			XSSFRow headerRow = worksheet.createRow(0);

			XSSFCellStyle headerCellStyle = workbook.createCellStyle();
			XSSFFont font = workbook.createFont();
			font.setFontName(XSSFFont.DEFAULT_FONT_NAME);
			
			// Thay đổi cách set màu cho font
			byte[] rgb = new byte[]{(byte)255, (byte)255, (byte)255};
			font.setColor(new XSSFColor(rgb, null));
			headerCellStyle.setFont(font);
			
			// Thay đổi cách set màu nền
			byte[] backgroundRgb = new byte[]{(byte)135, (byte)206, (byte)250};
			headerCellStyle.setFillForegroundColor(new XSSFColor(backgroundRgb, null));
			headerCellStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

			XSSFCell productId = headerRow.createCell(0);
			productId.setCellValue("Mã sản phẩm");
			productId.setCellStyle(headerCellStyle);

			XSSFCell productName = headerRow.createCell(1);
			productName.setCellValue("Tên sản phẩm");
			productName.setCellStyle(headerCellStyle);

			XSSFCell productBrand = headerRow.createCell(2);
			productBrand.setCellValue("Thương hiệu");
			productBrand.setCellStyle(headerCellStyle);

			XSSFCell price = headerRow.createCell(3);
			price.setCellValue("Giá nhập");
			price.setCellStyle(headerCellStyle);

			XSSFCell priceSell = headerRow.createCell(4);
			priceSell.setCellValue("Giá bán");
			priceSell.setCellStyle(headerCellStyle);

			XSSFCell createdAt = headerRow.createCell(5);
			createdAt.setCellValue("Ngày tạo");
			createdAt.setCellStyle(headerCellStyle);

			XSSFCell modifiedAt = headerRow.createCell(6);
			modifiedAt.setCellValue("Ngày sửa");
			modifiedAt.setCellStyle(headerCellStyle);

			XSSFCell totalSold = headerRow.createCell(7);
			totalSold.setCellValue("Đã bán");
			totalSold.setCellStyle(headerCellStyle);

			if (!products.isEmpty()) {
				for (int i = 0; i < products.size(); i++) {
					Product product = products.get(i);
					XSSFRow bodyRow = worksheet.createRow(i + 1);
					XSSFCellStyle bodyCellStyle = workbook.createCellStyle();
					
					// Thay đổi cách set màu nền cho body cells
					byte[] whiteBg = new byte[]{(byte)255, (byte)255, (byte)255};
					bodyCellStyle.setFillForegroundColor(new XSSFColor(whiteBg, null));

					XSSFCell productIDValue = bodyRow.createCell(0);
					productIDValue.setCellValue(product.getId());
					productIDValue.setCellStyle(bodyCellStyle);

					XSSFCell productNameValue = bodyRow.createCell(1);
					productNameValue.setCellValue(product.getName());
					productNameValue.setCellStyle(bodyCellStyle);

					XSSFCell productBrandValue = bodyRow.createCell(2);
					productBrandValue.setCellValue(product.getBrand().getName());
					productBrandValue.setCellStyle(bodyCellStyle);

					XSSFCell priceValue = bodyRow.createCell(3);
					priceValue.setCellValue(product.getPrice());
					priceValue.setCellStyle(bodyCellStyle);

					XSSFCell priceSellValue = bodyRow.createCell(4);
					priceSellValue.setCellValue(product.getSalePrice());
					priceSellValue.setCellStyle(bodyCellStyle);

					CreationHelper creationHelper = workbook.getCreationHelper();
					CellStyle cellStyle = workbook.createCellStyle();
					cellStyle.setDataFormat(creationHelper.createDataFormat().getFormat("dd/MM/yyyy HH:mm:ss"));

					XSSFCell createdAtValue = bodyRow.createCell(5);
					createdAtValue.setCellValue(product.getCreatedAt());
					createdAtValue.setCellStyle(cellStyle);

					XSSFCell updatedAtValue = bodyRow.createCell(6);
					updatedAtValue.setCellValue(product.getModifiedAt());
					updatedAtValue.setCellStyle(cellStyle);

					XSSFCell totalSoldValue = bodyRow.createCell(7);
					totalSoldValue.setCellValue(product.getTotalSold());
					totalSoldValue.setCellStyle(bodyCellStyle);
				}
			}
			workbook.write(fos);
			return file + "\\" + fileName + xlsx;
		} catch (Exception e) {
			return null;
		}
	}

	private void fileDownload(String fullPath, HttpServletResponse response, String fileName, String type) {
		File file = new File(fullPath);
		if (file.exists()) {
			OutputStream os = null;
			try (FileInputStream fis = new FileInputStream(file);) {
				String mimeType = context.getMimeType(fullPath);
				response.setContentType(mimeType);
				response.setHeader("content-disposition", "attachment; filename=" + fileName + "." + type);
				os = response.getOutputStream();
				byte[] buffer = new byte[BUFFER_SIZE];
				int bytesRead = -1;
				while ((bytesRead = fis.read(buffer)) != -1) {
					os.write(buffer, 0, bytesRead);
				}
				Files.delete(file.toPath());
			} catch (Exception e) {
				log.error("Can't download file, detail: {}", e.getMessage());
			} finally {
				if (os != null) {
					try {
						os.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		}
	}
}
