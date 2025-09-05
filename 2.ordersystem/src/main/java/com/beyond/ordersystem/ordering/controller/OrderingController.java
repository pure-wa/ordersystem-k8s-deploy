package com.beyond.ordersystem.ordering.controller;

import com.beyond.ordersystem.common.dto.CommonSuccessDto;
import com.beyond.ordersystem.ordering.dto.OrderCreateDto;
import com.beyond.ordersystem.ordering.dto.OrderListResDto;
import com.beyond.ordersystem.ordering.service.OrderingService;
import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/ordering")
public class OrderingController {
    private final OrderingService orderingService;

    // 주문 생성
    @PostMapping("/create")
    public ResponseEntity<?> createOrdering(@RequestBody @Valid List<OrderCreateDto> dto) {

        Long id = orderingService.createConcurrent(dto);

        return new ResponseEntity<>(new CommonSuccessDto(id, HttpStatus.OK.value(), "주문 성공"), HttpStatus.CREATED);
    }

    // 주문 목록 조회
    @GetMapping("/list")
    public ResponseEntity<?> findAll() {

        List<OrderListResDto> orderListResDtoList = orderingService.findAll();

        return new ResponseEntity<>(new CommonSuccessDto(orderListResDtoList, HttpStatus.OK.value(), "주문목록 조회 성공"), HttpStatus.OK);
    }

    @GetMapping("/myorders")
    public ResponseEntity<?> myOrders() {
        List<OrderListResDto> orderListResDtoList = orderingService.myorders();
        return new ResponseEntity<>(
                new CommonSuccessDto(
                        orderListResDtoList,
                        HttpStatus.OK.value(),
                        "주문목록 조회 성공"), HttpStatus.OK);
    }
    @DeleteMapping("/cancel/{orderId}")
    public ResponseEntity<?> orderCancel(@PathVariable Long orderId) throws JsonProcessingException {
        orderingService.orderCancel(orderId);
        return new ResponseEntity<>(
                new CommonSuccessDto(null,HttpStatus.OK.value(), "주문취소성공"),
                HttpStatus.OK
        );
    }



}
