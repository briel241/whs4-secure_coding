package com.tinyshop.product;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tinyshop.PostgresIntegrationTest;
import com.tinyshop.common.exception.BusinessException;
import com.tinyshop.product.domain.ItemCondition;
import com.tinyshop.product.domain.SaleStatus;
import com.tinyshop.product.domain.TradeMethod;
import com.tinyshop.product.dto.ProductForm;
import com.tinyshop.product.repository.CategoryRepository;
import com.tinyshop.product.service.ProductService;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.List;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;

class ImageUploadIntegrationTest extends PostgresIntegrationTest {
    @Autowired ProductService products;
    @Autowired CategoryRepository categories;

    @Test
    void validPngIsDecodedAndReencoded() throws Exception {
        var seller = register("image-valid");
        var product = products.create(seller.getId(), form(),
            List.of(new MockMultipartFile("images", "photo.png", "image/png", png())));
        var images = products.images(product.getId());
        assertThat(images).hasSize(1);
        assertThat(images.getFirst().getContentType()).isEqualTo("image/png");
        assertThat(images.getFirst().getStorageKey()).matches("[0-9a-f-]{36}\\.png");
    }

    @Test
    void rejectsDisguisedMimeDoubleExtensionTraversalSvgAndCorruption() {
        var seller = register("image-bad");
        assertRejected(seller, new MockMultipartFile("images", "photo.jpg", "image/jpeg", new byte[]{1, 2, 3}));
        assertRejected(seller, new MockMultipartFile("images", "photo.jpg.exe", "image/jpeg", new byte[]{1, 2, 3}));
        assertRejected(seller, new MockMultipartFile("images", "../photo.png", "image/png", new byte[]{1, 2, 3}));
        assertRejected(seller, new MockMultipartFile("images", "photo.svg", "image/svg+xml",
            "<svg/>".getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        assertRejected(seller, new MockMultipartFile("images", "broken.png", "image/png",
            new byte[]{(byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a}));
        assertRejected(seller, new MockMultipartFile("images", "huge.png", "image/png",
            new byte[5 * 1024 * 1024 + 1]));
    }

    @Test
    void rejectsExcessivePixelDimensions() throws Exception {
        var seller = register("image-pixels");
        BufferedImage image = new BufferedImage(8001, 1, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(image, "png", output);
        assertRejected(seller, new MockMultipartFile("images", "wide.png", "image/png", output.toByteArray()));
    }

    private void assertRejected(com.tinyshop.user.domain.User seller, MockMultipartFile file) {
        assertThatThrownBy(() -> products.create(seller.getId(), form(), List.of(file)))
            .isInstanceOf(BusinessException.class);
    }

    private byte[] png() throws Exception {
        BufferedImage image = new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(image, "png", output);
        return output.toByteArray();
    }

    private ProductForm form() {
        ProductForm form = new ProductForm();
        form.setTitle("이미지 상품");
        form.setDescription("설명");
        form.setPrice(100);
        form.setCategoryId(categories.findByActiveTrueOrderByName().getFirst().getId());
        form.setItemCondition(ItemCondition.GOOD);
        form.setTradeMethod(TradeMethod.BOTH);
        form.setSaleStatus(SaleStatus.ON_SALE);
        return form;
    }
}
