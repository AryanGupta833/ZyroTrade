package com.ZyroTrade.Orders;

import com.ZyroTrade.Orders.Order;
import com.ZyroTrade.Orders.OrderService;
import com.ZyroTrade.Orders.OrderType;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public Order placeOrder(
            @RequestParam Long stockId,
            @RequestParam OrderType type,
            @RequestParam int quantity) {

        return orderService.placeOrder(stockId, type, quantity);
    }
    @GetMapping
    public List<Order> myOrders(){
        return orderService.myOrders();
    }
}
