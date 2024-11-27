package com.web.application.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import com.web.application.config.Contant;
import com.web.application.entity.Brand;
import com.web.application.exception.BadRequestExp;
import com.web.application.exception.InternalServerExp;
import com.web.application.exception.NotFoundExp;
import com.web.application.model.mapper.BrandMapper;
import com.web.application.model.request.CreateBrandRequest;
import com.web.application.repository.BrandRepository;
import com.web.application.service.BrandService;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

@Component
public class BrandServiceImpl implements BrandService {

	@Autowired
	private BrandRepository brandRepository;

	@Override
	public Page<Brand> adminGetListBrands(String id, String name, String status, Integer page) {
		page--;
		if (page < 0) {
			page = 0;
		}
		if (status.equalsIgnoreCase("ẩn")) {
			status = "0";
		} else if(status.equalsIgnoreCase("hiện")) {
			status = "1";
		}
		Pageable pageable = PageRequest.of(page, Contant.LIMIT_BRAND, Sort.by("created_at").descending());
		return brandRepository.adminGetListBrands(id, name, status, pageable);

	}

	@Override
	public List<Brand> getListBrand() {
		return brandRepository.findAll();
	}

	@Override
	public Brand createBrand(CreateBrandRequest createBrandRequest) {
		Brand brand = brandRepository.findByName(createBrandRequest.getName());
		if (brand != null) {
			throw new BadRequestExp("Tên nhãn hiệu đã tồn tại trong hệ thống, Vui lòng chọn tên khác!");
		}
		brand = BrandMapper.toBrand(createBrandRequest);
		brandRepository.save(brand);
		return brand;
	}

	@Override
	public void updateBrand(CreateBrandRequest createBrandRequest, Long id) {
		Optional<Brand> brand = brandRepository.findById(id);
		if (brand.isEmpty()) {
			throw new NotFoundExp("Tên nhãn hiệu không tồn tại!");
		}
		Brand br = brandRepository.findByName(createBrandRequest.getName());
		if (br != null) {
			if (!createBrandRequest.getId().equals(br.getId()))
				throw new BadRequestExp("Tên nhãn hiệu " + createBrandRequest.getName()
						+ " đã tồn tại trong hệ thống, Vui lòng chọn tên khác!");
		}
		Brand rs = brand.get();
		System.out.println(rs.getName());
		rs.setId(id);
		rs.setName(createBrandRequest.getName());
		rs.setDescription(createBrandRequest.getDescription());
		rs.setThumbnail(createBrandRequest.getThumbnail());
		rs.setStatus(createBrandRequest.isStatus());
		rs.setModifiedAt(new Timestamp(System.currentTimeMillis()));

		try {
			brandRepository.save(rs);
		} catch (Exception ex) {
			throw new InternalServerExp("Lỗi khi chỉnh sửa nhãn hiệu");
		}
	}

	@Override
	public void deleteBrand(long id) {
		Optional<Brand> brand = brandRepository.findById(id);
		if (brand.isEmpty()) {
			throw new NotFoundExp("Tên nhãn hiệu không tồn tại!");
		}
		try {
			brandRepository.deleteById(id);
		} catch (Exception ex) {
			throw new InternalServerExp("Lỗi khi xóa nhãn hiệu!");
		}
	}

	@Override
	public Brand getBrandById(long id) {
		Optional<Brand> brand = brandRepository.findById(id);
		if (brand.isEmpty()) {
			throw new NotFoundExp("Tên nhãn hiệu không tồn tại!");
		}
		return brand.get();
	}

	@Override
	public long getCountBrands() {
		return brandRepository.count();
	}
}
