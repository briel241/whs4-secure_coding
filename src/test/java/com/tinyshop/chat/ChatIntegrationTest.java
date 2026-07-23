package com.tinyshop.chat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tinyshop.PostgresIntegrationTest;
import com.tinyshop.auth.security.PlatformPrincipal;
import com.tinyshop.chat.service.ChatService;
import com.tinyshop.common.exception.BusinessException;
import com.tinyshop.common.exception.NotFoundException;
import com.tinyshop.product.domain.ItemCondition;
import com.tinyshop.product.domain.SaleStatus;
import com.tinyshop.product.domain.TradeMethod;
import com.tinyshop.product.dto.ProductForm;
import com.tinyshop.product.repository.CategoryRepository;
import com.tinyshop.product.service.ProductService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

class ChatIntegrationTest extends PostgresIntegrationTest {
    @Autowired ChatService chats;
    @Autowired ProductService products;
    @Autowired CategoryRepository categories;
    @Autowired MockMvc mvc;

    @Test
    void uniqueConversationParticipantChecksUnreadAndPlainText() {
        var seller = register("chat-seller");
        var buyer = register("chat-buyer");
        var outsider = register("chat-outsider");
        var product = products.create(seller.getId(), form(), List.of());
        var first = chats.open(product.getId(), buyer.getId());
        var duplicate = chats.open(product.getId(), buyer.getId());
        assertThat(duplicate.getId()).isEqualTo(first.getId());
        assertThatThrownBy(() -> chats.visibleMessages(first.getId(), outsider.getId()))
            .isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> chats.open(product.getId(), seller.getId()))
            .isInstanceOf(BusinessException.class);

        chats.view(first.getId(), buyer.getId());
        chats.send(first.getId(), seller.getId(), "<script>alert(1)</script>");
        assertThat(chats.unread(first.getId(), buyer.getId())).isEqualTo(1);
        assertThat(chats.visibleMessages(first.getId(), buyer.getId()).getFirst().getBody())
            .isEqualTo("<script>alert(1)</script>");
    }

    @Test
    void excessiveMessagesReturn429BusinessError() {
        var seller = register("rate-seller");
        var buyer = register("rate-buyer");
        var product = products.create(seller.getId(), form(), List.of());
        var conversation = chats.open(product.getId(), buyer.getId());
        for (int i = 0; i < 10; i++) chats.send(conversation.getId(), buyer.getId(), "메시지 " + i);
        assertThatThrownBy(() -> chats.send(conversation.getId(), buyer.getId(), "초과"))
            .isInstanceOf(BusinessException.class)
            .extracting("status").isEqualTo(org.springframework.http.HttpStatus.TOO_MANY_REQUESTS);
    }

    @Test
    void conversationShowsCounterpartyAndMessageReportActions() throws Exception {
        var seller = register("report-link-seller");
        var buyer = register("report-link-buyer");
        var product = products.create(seller.getId(), form(), List.of());
        var conversation = chats.open(product.getId(), buyer.getId());
        var message = chats.send(conversation.getId(), seller.getId(), "신고 링크 확인 메시지");

        mvc.perform(get("/conversations/" + conversation.getId())
                .with(user(new PlatformPrincipal(buyer))))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("targetType=USER")))
            .andExpect(content().string(containsString(seller.getId().toString())))
            .andExpect(content().string(containsString("targetType=MESSAGE")))
            .andExpect(content().string(containsString(message.getId().toString())));
    }

    private ProductForm form() {
        ProductForm form = new ProductForm();
        form.setTitle("대화 상품");
        form.setDescription("설명");
        form.setPrice(100);
        form.setCategoryId(categories.findByActiveTrueOrderByName().getFirst().getId());
        form.setItemCondition(ItemCondition.GOOD);
        form.setTradeMethod(TradeMethod.DIRECT);
        form.setSaleStatus(SaleStatus.ON_SALE);
        return form;
    }
}
