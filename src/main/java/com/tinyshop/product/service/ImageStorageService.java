package com.tinyshop.product.service;

import com.tinyshop.common.exception.BusinessException;
import com.tinyshop.common.exception.NotFoundException;
import com.tinyshop.config.AppProperties;
import com.tinyshop.product.domain.Product;
import com.tinyshop.product.domain.ProductImage;
import com.tinyshop.product.repository.ProductImageRepository;
import jakarta.annotation.PostConstruct;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import javax.imageio.ImageIO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class ImageStorageService {
    private static final Logger log = LoggerFactory.getLogger(ImageStorageService.class);
    private static final Set<String> CONTENT_TYPES = Set.of("image/jpeg", "image/png", "image/webp");
    private final AppProperties properties;
    private final ProductImageRepository images;
    private Path root;

    public ImageStorageService(AppProperties properties, ProductImageRepository images) {
        this.properties = properties;
        this.images = images;
    }

    @PostConstruct
    void initialize() throws IOException {
        root = properties.upload().directory().toAbsolutePath().normalize();
        Files.createDirectories(root);
    }

    @Transactional
    public List<ProductImage> store(Product product, List<MultipartFile> files) {
        List<MultipartFile> actual = files == null ? List.of()
            : files.stream().filter(file -> file != null && !file.isEmpty()).toList();
        long existing = images.countByProductId(product.getId());
        if (existing + actual.size() > 5) throw new BusinessException("상품 이미지는 최대 5개입니다.");
        List<Path> written = new ArrayList<>();
        List<ProductImage> result = new ArrayList<>();
        try {
            int order = (int) existing;
            for (MultipartFile file : actual) {
                ValidatedImage validated = validate(file);
                String key = UUID.randomUUID() + ".png";
                Path target = root.resolve(key).normalize();
                if (!target.getParent().equals(root)) throw new BusinessException("안전하지 않은 저장 경로입니다.");
                Path temporary = Files.createTempFile(root, "upload-", ".tmp");
                try {
                    Files.write(temporary, validated.bytes());
                    Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE);
                } finally {
                    Files.deleteIfExists(temporary);
                }
                written.add(target);
                result.add(images.save(new ProductImage(product, key, "image/png",
                    validated.bytes().length, validated.width(), validated.height(), order++)));
            }
            return result;
        } catch (IOException | RuntimeException exception) {
            written.forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException cleanupFailure) {
                    log.warn("Could not clean rejected upload key={}", path.getFileName());
                }
            });
            if (exception instanceof BusinessException business) throw business;
            throw new BusinessException("이미지를 안전하게 저장하지 못했습니다.");
        }
    }

    @Transactional(readOnly = true)
    public StoredImage load(String key) {
        if (key == null || !key.matches("[0-9a-fA-F-]{36}\\.png")) {
            throw new NotFoundException("Image not found");
        }
        ProductImage image = images.findByStorageKey(key)
            .orElseThrow(() -> new NotFoundException("Image not found"));
        Path path = root.resolve(image.getStorageKey()).normalize();
        if (!path.getParent().equals(root)) throw new NotFoundException("Image not found");
        try {
            return new StoredImage(Files.readAllBytes(path), image.getContentType());
        } catch (IOException exception) {
            throw new NotFoundException("Image not found");
        }
    }

    private ValidatedImage validate(MultipartFile file) throws IOException {
        String name = file.getOriginalFilename();
        if (name == null || name.contains("..") || name.contains("/") || name.contains("\\")
            || !name.matches("(?iu)^[\\p{L}\\p{N}_ -]+\\.(jpg|jpeg|png|webp)$")) {
            throw new BusinessException("안전한 JPG, PNG, WebP 파일명만 사용할 수 있습니다.");
        }
        if (file.getSize() <= 0 || file.getSize() > properties.upload().maxFileBytes()) {
            throw new BusinessException("이미지 크기는 5MB 이하여야 합니다.");
        }
        String contentType = file.getContentType() == null ? "" : file.getContentType().toLowerCase(Locale.ROOT);
        if (!CONTENT_TYPES.contains(contentType)) throw new BusinessException("허용하지 않는 이미지 MIME 형식입니다.");
        byte[] original = file.getBytes();
        String signature = signature(original);
        if (!signature.equals(contentType)) throw new BusinessException("확장자, MIME, 실제 파일 형식이 일치하지 않습니다.");
        String extension = name.substring(name.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
        if (!extensionMatches(extension, signature)) throw new BusinessException("확장자와 실제 파일 형식이 일치하지 않습니다.");
        BufferedImage decoded = ImageIO.read(new ByteArrayInputStream(original));
        if (decoded == null) throw new BusinessException("손상되었거나 디코딩할 수 없는 이미지입니다.");
        int width = decoded.getWidth();
        int height = decoded.getHeight();
        long pixels = Math.multiplyExact((long) width, height);
        if (width <= 0 || height <= 0 || width > properties.upload().maxWidth()
            || height > properties.upload().maxHeight() || pixels > properties.upload().maxPixels()) {
            throw new BusinessException("이미지 픽셀 크기가 허용 범위를 초과합니다.");
        }
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        if (!ImageIO.write(decoded, "png", output)) throw new BusinessException("이미지를 재인코딩하지 못했습니다.");
        if (output.size() > properties.upload().maxFileBytes()) {
            throw new BusinessException("안전한 형식으로 변환한 이미지가 5MB를 초과합니다.");
        }
        return new ValidatedImage(output.toByteArray(), width, height);
    }

    private String signature(byte[] bytes) {
        if (bytes.length >= 3 && (bytes[0] & 0xff) == 0xff && (bytes[1] & 0xff) == 0xd8
            && (bytes[2] & 0xff) == 0xff) return "image/jpeg";
        if (bytes.length >= 8 && (bytes[0] & 0xff) == 0x89 && bytes[1] == 0x50
            && bytes[2] == 0x4e && bytes[3] == 0x47 && bytes[4] == 0x0d
            && bytes[5] == 0x0a && bytes[6] == 0x1a && bytes[7] == 0x0a) return "image/png";
        if (bytes.length >= 12 && bytes[0] == 'R' && bytes[1] == 'I' && bytes[2] == 'F'
            && bytes[3] == 'F' && bytes[8] == 'W' && bytes[9] == 'E'
            && bytes[10] == 'B' && bytes[11] == 'P') return "image/webp";
        return "";
    }

    private boolean extensionMatches(String extension, String signature) {
        return switch (signature) {
            case "image/jpeg" -> extension.equals("jpg") || extension.equals("jpeg");
            case "image/png" -> extension.equals("png");
            case "image/webp" -> extension.equals("webp");
            default -> false;
        };
    }

    private record ValidatedImage(byte[] bytes, int width, int height) {
    }

    public record StoredImage(byte[] bytes, String contentType) {
    }
}
