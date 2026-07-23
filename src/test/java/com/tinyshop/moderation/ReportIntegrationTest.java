package com.tinyshop.moderation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tinyshop.PostgresIntegrationTest;
import com.tinyshop.chat.service.ChatService;
import com.tinyshop.common.exception.BusinessException;
import com.tinyshop.common.exception.NotFoundException;
import com.tinyshop.moderation.domain.ReportTargetType;
import com.tinyshop.moderation.domain.ReportType;
import com.tinyshop.moderation.dto.ReportForm;
import com.tinyshop.moderation.service.ReportService;
import com.tinyshop.product.domain.ItemCondition;
import com.tinyshop.product.domain.SaleStatus;
import com.tinyshop.product.domain.TradeMethod;
import com.tinyshop.product.dto.ProductForm;
import com.tinyshop.product.repository.CategoryRepository;
import com.tinyshop.product.service.ProductService;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class ReportIntegrationTest extends PostgresIntegrationTest {
    @Autowired ReportService reports;
    @Autowired ChatService chats;
    @Autowired ProductService products;
    @Autowired CategoryRepository categories;

    @Test
    void participantCanReportCounterpartyAndReceivedMessageButNotOwnOrUnrelatedMessage() {
        var seller = register("report-seller");
        var buyer = register("report-buyer");
        var outsider = register("report-outsider");
        var product = products.create(seller.getId(), productForm(), List.of());
        var conversation = chats.open(product.getId(), buyer.getId());
        var message = chats.send(conversation.getId(), seller.getId(), "부적절한 메시지");

        var userReport = reports.create(buyer.getId(),
            reportForm(ReportTargetType.USER, seller.getId()));
        var messageReport = reports.create(buyer.getId(),
            reportForm(ReportTargetType.MESSAGE, message.getId()));

        assertThat(userReport.getTargetType()).isEqualTo(ReportTargetType.USER);
        assertThat(messageReport.getTargetType()).isEqualTo(ReportTargetType.MESSAGE);
        assertThatThrownBy(() -> reports.create(seller.getId(),
            reportForm(ReportTargetType.MESSAGE, message.getId())))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("자신이 보낸 메시지");
        assertThatThrownBy(() -> reports.create(outsider.getId(),
            reportForm(ReportTargetType.MESSAGE, message.getId())))
            .isInstanceOf(NotFoundException.class);
    }

    private ReportForm reportForm(ReportTargetType targetType, UUID targetId) {
        ReportForm form = new ReportForm();
        form.setTargetType(targetType);
        form.setTargetId(targetId);
        form.setReportType(ReportType.HARASSMENT);
        form.setDetails("정책 위반 여부를 확인해 주세요.");
        return form;
    }

    private ProductForm productForm() {
        ProductForm form = new ProductForm();
        form.setTitle("신고 테스트 상품");
        form.setDescription("설명");
        form.setPrice(100);
        form.setCategoryId(categories.findByActiveTrueOrderByName().getFirst().getId());
        form.setItemCondition(ItemCondition.GOOD);
        form.setTradeMethod(TradeMethod.DIRECT);
        form.setSaleStatus(SaleStatus.ON_SALE);
        return form;
    }
}
