package com.tinyshop.product;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tinyshop.PostgresIntegrationTest;
import com.tinyshop.auth.security.PlatformPrincipal;
import com.tinyshop.common.exception.NotFoundException;
import com.tinyshop.product.domain.ItemCondition;
import com.tinyshop.product.domain.SaleStatus;
import com.tinyshop.product.domain.TradeMethod;
import com.tinyshop.product.dto.ProductForm;
import com.tinyshop.product.dto.ProductSearch;
import com.tinyshop.product.repository.CategoryRepository;
import com.tinyshop.product.service.ProductService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

class ProductIntegrationTest extends PostgresIntegrationTest {
    @Autowired ProductService products;
    @Autowired CategoryRepository categories;
    @Autowired MockMvc mvc;

    @Test
    void ownerCanCreateButOtherUserCannotUpdateOrDelete() {
        var seller = register("seller");
        var outsider = register("outsider");
        var product = products.create(seller.getId(), form("안전한 상품", "일반 설명", 10_000), List.of());
        assertThat(products.detail(product.getId(), null).getTitle()).isEqualTo("안전한 상품");
        assertThatThrownBy(() -> products.update(product.getId(), outsider.getId(),
            form("탈취 수정", "설명", 1), List.of())).isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> products.delete(product.getId(), outsider.getId()))
            .isInstanceOf(NotFoundException.class);
    }

    @Test
    void deletedProductAndSuspendedSellerAreNotPublicAndSearchIsBounded() {
        var seller = register("seller-hidden");
        var deleted = products.create(seller.getId(), form("삭제 상품", "설명", 100), List.of());
        products.delete(deleted.getId(), seller.getId());
        assertThatThrownBy(() -> products.detail(deleted.getId(), null)).isInstanceOf(NotFoundException.class);

        ProductSearch search = new ProductSearch();
        search.setQuery("' OR 1=1 --");
        search.setSort("createdAt;drop table users");
        search.setSize(9999);
        assertThat(products.search(search).getSize()).isEqualTo(50);
    }

    @Test
    void xssDescriptionIsEscapedByThymeleaf() throws Exception {
        var seller = register("xss-seller");
        var product = products.create(seller.getId(),
            form("이스케이프 상품", "<script>alert(1)</script>", 500), List.of());
        mvc.perform(get("/products/{id}", product.getId()))
            .andExpect(status().isOk())
            .andExpect(content().string(not(containsString("<script>alert(1)</script>"))))
            .andExpect(content().string(containsString("&lt;script&gt;alert(1)&lt;/script&gt;")));
    }

    @Test
    void negativePriceIsRejectedAtRequestBoundary() throws Exception {
        var seller = register("negative-price");
        mvc.perform(post("/products").with(user(new PlatformPrincipal(seller))).with(csrf())
                .param("title", "정상 제목")
                .param("description", "설명")
                .param("price", "-1")
                .param("categoryId", categories.findByActiveTrueOrderByName().getFirst().getId().toString())
                .param("itemCondition", "GOOD")
                .param("tradeMethod", "DIRECT")
                .param("saleStatus", "ON_SALE"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("가격은 0 이상")));
    }

    @Test
    void commaFormattedWonPriceIsAcceptedAndRendered() throws Exception {
        var seller = register("comma-price");
        mvc.perform(post("/products").with(user(new PlatformPrincipal(seller))).with(csrf())
                .param("title", "쉼표 가격 상품")
                .param("description", "가격 입력 서식 확인")
                .param("price", "1,234,000")
                .param("categoryId", categories.findByActiveTrueOrderByName().getFirst().getId().toString())
                .param("itemCondition", "GOOD")
                .param("tradeMethod", "DIRECT")
                .param("saleStatus", "ON_SALE"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrlPattern("/products/*"));

        mvc.perform(get("/products")
                .param("minPrice", "1,000,000")
                .param("maxPrice", "2,000,000"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("최소 가격 (원)")))
            .andExpect(content().string(containsString("쉼표 가격 상품")))
            .andExpect(content().string(containsString("1,234,000원")));
    }

    private ProductForm form(String title, String description, long price) {
        ProductForm form = new ProductForm();
        form.setTitle(title);
        form.setDescription(description);
        form.setPrice(price);
        form.setCategoryId(categories.findByActiveTrueOrderByName().getFirst().getId());
        form.setItemCondition(ItemCondition.GOOD);
        form.setTradeMethod(TradeMethod.DIRECT);
        form.setTradeRegion("서울");
        form.setSaleStatus(SaleStatus.ON_SALE);
        return form;
    }
}
