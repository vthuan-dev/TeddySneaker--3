package com.web.application.controller.admin;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import com.web.application.entity.Brand;
import com.web.application.entity.User;
import com.web.application.model.mapper.BrandMapper;
import com.web.application.model.request.CreateBrandRequest;
import com.web.application.security.CustomUserDetails;
import com.web.application.service.BrandService;
import com.web.application.service.ImageService;

import javax.validation.Valid;
import java.util.List;
import java.util.Objects;

@Controller
public class BrandController {

	@Autowired
	private BrandService brandService;

	@Autowired
	private ImageService imageService;

	@GetMapping("/admin/brands")
	public String homePage(Model model, @RequestParam(defaultValue = "", required = false) String keyword,
			@RequestParam(defaultValue = "1", required = false) Integer page) {
		// Lấy tất cả các anh của user upload
		User user = ((CustomUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal())
				.getUser();
		List<String> images = imageService.getListImageOfUser(user.getId());
		model.addAttribute("images", images);

		Page<Brand> brands = brandService.adminGetListBrands(keyword, keyword, keyword, page);
		model.addAttribute("brands", brands.getContent());
		model.addAttribute("totalPages", brands.getTotalPages());
		model.addAttribute("currentPage", brands.getPageable().getPageNumber() + 1);
		return "admin/brand/list";
	}

	@PostMapping("/api/admin/brands")
	public ResponseEntity<Object> createBrand(@Valid @RequestBody CreateBrandRequest createBrandRequest) {
		Brand brand = brandService.createBrand(createBrandRequest);
		return ResponseEntity.ok(BrandMapper.toBrandDTO(brand));
	}

	@PutMapping("/api/admin/brands/{id}")
	public ResponseEntity<Object> updateBrand(@Valid @RequestBody CreateBrandRequest createBrandRequest,
			@PathVariable long id) {
		brandService.updateBrand(createBrandRequest, id);
		return ResponseEntity.ok("Sửa nhãn hiệu thành công!");
	}

	@DeleteMapping("/api/admin/brands/{id}")
	public ResponseEntity<Object> deleteBrand(@PathVariable long id) {
		brandService.deleteBrand(id);
		return ResponseEntity.ok("Xóa nhãn hiệu thành công!");
	}

	@GetMapping("/api/admin/brands/{id}")
	public ResponseEntity<Object> getBrandById(@PathVariable long id) {
		Brand brand = brandService.getBrandById(id);
		return ResponseEntity.ok(brand);
	}
}
