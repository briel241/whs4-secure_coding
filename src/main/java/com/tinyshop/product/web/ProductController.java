package com.tinyshop.product.web;

import com.tinyshop.auth.security.PlatformPrincipal;
import com.tinyshop.common.exception.BusinessException;
import com.tinyshop.product.domain.ItemCondition;
import com.tinyshop.product.domain.SaleStatus;
import com.tinyshop.product.domain.TradeMethod;
import com.tinyshop.product.dto.ProductForm;
import com.tinyshop.product.dto.ProductSearch;
import com.tinyshop.product.service.ImageStorageService;
import com.tinyshop.product.service.ProductService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

@Controller
public class ProductController {
    private final ProductService products;
    private final ImageStorageService imageStorage;

    public ProductController(ProductService products, ImageStorageService imageStorage) {
        this.products = products;
        this.imageStorage = imageStorage;
    }

    @GetMapping("/")
    public String home(Model model) {
        ProductSearch search = new ProductSearch();
        search.setSize(6);
        model.addAttribute("products", products.search(search).getContent());
        return "home";
    }

    @GetMapping("/products")
    public String list(@ModelAttribute("search") ProductSearch search, Model model) {
        model.addAttribute("page", products.search(search));
        model.addAttribute("categories", products.activeCategories());
        model.addAttribute("statuses", List.of(SaleStatus.ON_SALE, SaleStatus.RESERVED, SaleStatus.SOLD));
        model.addAttribute("tradeMethods", TradeMethod.values());
        return "product/list";
    }

    @GetMapping("/products/{id}")
    public String detail(@PathVariable UUID id,
                         @AuthenticationPrincipal PlatformPrincipal principal, Model model) {
        var product = products.detail(id, principal == null ? null : principal.id());
        model.addAttribute("product", product);
        model.addAttribute("images", products.images(id));
        model.addAttribute("owner", principal != null && product.getSeller().getId().equals(principal.id()));
        return "product/detail";
    }

    @GetMapping("/products/new")
    public String createForm(Model model) {
        model.addAttribute("form", new ProductForm());
        formOptions(model);
        return "product/form";
    }

    @PostMapping("/products")
    public String create(@AuthenticationPrincipal PlatformPrincipal principal,
                         @Valid @ModelAttribute("form") ProductForm form, BindingResult errors,
                         @RequestParam(name = "images", required = false) List<MultipartFile> files,
                         Model model) {
        if (errors.hasErrors()) {
            formOptions(model);
            return "product/form";
        }
        try {
            return "redirect:/products/" + products.create(principal.id(), form, files).getId();
        } catch (BusinessException exception) {
            errors.reject("images", exception.getMessage());
            formOptions(model);
            return "product/form";
        }
    }

    @GetMapping("/products/{id}/edit")
    public String editForm(@PathVariable UUID id, @AuthenticationPrincipal PlatformPrincipal principal,
                           Model model) {
        var product = products.detail(id, principal.id());
        if (!product.getSeller().getId().equals(principal.id())) {
            throw new com.tinyshop.common.exception.NotFoundException("Product not found");
        }
        model.addAttribute("form", ProductForm.from(product));
        model.addAttribute("product", product);
        model.addAttribute("existingImages", products.images(id));
        formOptions(model);
        return "product/form";
    }

    @PostMapping("/products/{id}")
    public String update(@PathVariable UUID id, @AuthenticationPrincipal PlatformPrincipal principal,
                         @Valid @ModelAttribute("form") ProductForm form, BindingResult errors,
                         @RequestParam(name = "images", required = false) List<MultipartFile> files,
                         Model model) {
        if (errors.hasErrors()) {
            model.addAttribute("product", products.detail(id, principal.id()));
            model.addAttribute("existingImages", products.images(id));
            formOptions(model);
            return "product/form";
        }
        try {
            products.update(id, principal.id(), form, files);
            return "redirect:/products/" + id + "?updated";
        } catch (BusinessException exception) {
            errors.reject("images", exception.getMessage());
            model.addAttribute("product", products.detail(id, principal.id()));
            model.addAttribute("existingImages", products.images(id));
            formOptions(model);
            return "product/form";
        }
    }

    @PostMapping("/products/{id}/status")
    public String status(@PathVariable UUID id, @RequestParam SaleStatus status,
                         @AuthenticationPrincipal PlatformPrincipal principal) {
        products.changeStatus(id, principal.id(), status);
        return "redirect:/products/" + id;
    }

    @PostMapping("/products/{id}/delete")
    public String delete(@PathVariable UUID id, @AuthenticationPrincipal PlatformPrincipal principal) {
        products.delete(id, principal.id());
        return "redirect:/my/products?deleted";
    }

    @GetMapping("/my/products")
    public String mine(@RequestParam(defaultValue = "0") int page,
                       @AuthenticationPrincipal PlatformPrincipal principal, Model model) {
        model.addAttribute("page", products.mine(principal.id(), page));
        return "product/mine";
    }

    @GetMapping("/images/{key}")
    public ResponseEntity<byte[]> image(@PathVariable String key) {
        var image = imageStorage.load(key);
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(image.contentType()))
            .cacheControl(CacheControl.noCache())
            .header("X-Content-Type-Options", "nosniff")
            .body(image.bytes());
    }

    private void formOptions(Model model) {
        model.addAttribute("categories", products.activeCategories());
        model.addAttribute("conditions", ItemCondition.values());
        model.addAttribute("tradeMethods", TradeMethod.values());
        model.addAttribute("saleStatuses", List.of(SaleStatus.DRAFT, SaleStatus.ON_SALE,
            SaleStatus.RESERVED, SaleStatus.SOLD));
    }
}
