package com.resto.catalog.controller;

import com.resto.catalog.domain.Product;
import com.resto.catalog.repository.ProductRepository;
import com.resto.core.response.Response;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/catalog")
public class Stock86Controller {

    private final ProductRepository productRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public Stock86Controller(ProductRepository productRepository, SimpMessagingTemplate messagingTemplate) {
        this.productRepository = productRepository;
        this.messagingTemplate = messagingTemplate;
    }

    @PostMapping("/products/{productId}/86")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'STORE_MANAGER')")
    public Response<Product> toggleStock86(@PathVariable("productId") UUID productId,
                                          @RequestBody Toggle86Request request) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + productId));

        product.setIs86(request.getIs86());
        Product updatedProduct = productRepository.save(product);

        // Broadcast real-time STOMP notification to /topic/store/{storeId}/pos
        if (request.getStoreId() != null) {
            String destination = "/topic/store/" + request.getStoreId() + "/pos";
            messagingTemplate.convertAndSend(destination, Map.of(
                    "type", "STOCK_86_TOGGLE",
                    "productId", productId,
                    "is86", request.getIs86()
            ));
        }

        return Response.<Product>builder()
                .status(HttpStatus.OK)
                .statusCode(HttpStatus.OK.value())
                .message("default.message.success")
                .service("RESTO-OS")
                .data(updatedProduct)
                .build();
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Toggle86Request {
        private UUID storeId;
        private Boolean is86;
    }
}
