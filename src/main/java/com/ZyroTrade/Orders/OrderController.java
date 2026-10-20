package com.ZyroTrade.Orders;

import com.ZyroTrade.DTO.OrderResponse;
import com.ZyroTrade.DTO.PageResponse;
import jakarta.validation.constraints.Min;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@Validated
@io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "bearerAuth")
@io.swagger.v3.oas.annotations.tags.Tag(name = "Orders", description = "Synchronous market orders and user-owned order history")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    @io.swagger.v3.oas.annotations.Operation(summary = "Execute a market order",
            description = "Executes BUY or SELL synchronously against a fresh provider quote. Positive integer share quantity is required. A matching Idempotency-Key returns the original result; reusing it with a different payload returns 409. Requests without the header are independent.")
    public OrderResponse placeOrder(
            @RequestParam Long stockId,
            @RequestParam OrderType type,
            @RequestParam @Min(1) int quantity,
            @io.swagger.v3.oas.annotations.Parameter(description = "Optional stable key for safe retry; maximum 128 characters")
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {

        return OrderResponse.from(orderService.placeOrder(stockId, type, quantity, idempotencyKey));
    }
    @GetMapping
    public List<OrderResponse> myOrders(){
        return orderService.myOrders().stream().map(OrderResponse::from).toList();
    }

    @GetMapping("/page")
    @io.swagger.v3.oas.annotations.Operation(summary = "Get paginated order history", description = "Pages are zero-based; size is capped at 100 and results are newest first.")
    public PageResponse<OrderResponse> myOrdersPage(@RequestParam(defaultValue = "0") @Min(0) int page,
                                                     @RequestParam(defaultValue = "20") @Min(1) int size,
                                                     @AuthenticationPrincipal UserDetails user) {
        int boundedSize = Math.min(size, 100);
        return PageResponse.from(orderService.myOrders(user.getUsername(),
                        PageRequest.of(page, boundedSize, Sort.by(Sort.Direction.DESC, "createdAt")))
                .map(OrderResponse::from));
    }

    @GetMapping("/{id}")
    @io.swagger.v3.oas.annotations.Operation(summary = "Get an order owned by the authenticated user")
    public OrderResponse orderDetails(@PathVariable Long id, @AuthenticationPrincipal UserDetails user) {
        return OrderResponse.from(orderService.myOrder(id, user.getUsername())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found")));
    }
}
