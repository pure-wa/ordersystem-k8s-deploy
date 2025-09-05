package com.beyond.ordersystem.ordering.service;

import com.beyond.ordersystem.common.service.SseAlarmService;
import com.beyond.ordersystem.common.service.StockInventoryService;
import com.beyond.ordersystem.member.domain.Member;
import com.beyond.ordersystem.member.repository.MemberRepository;
import com.beyond.ordersystem.ordering.domain.OrderDetail;
import com.beyond.ordersystem.ordering.domain.OrderStatus;
import com.beyond.ordersystem.ordering.domain.Ordering;
import com.beyond.ordersystem.ordering.dto.OrderCreateDto;
import com.beyond.ordersystem.ordering.dto.OrderDetailDto;
import com.beyond.ordersystem.ordering.dto.OrderListResDto;
import com.beyond.ordersystem.ordering.repository.OrderingDetailRepository;
import com.beyond.ordersystem.ordering.repository.OrderingRepository;
import com.beyond.ordersystem.product.domain.Product;
import com.beyond.ordersystem.product.repository.ProductRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class OrderingService {
    private final OrderingRepository orderingRepository;
    private final MemberRepository memberRepository;
    private final ProductRepository productRepository;
    private final OrderingDetailRepository orderingDetailRepository;
    private final StockInventoryService stockInventoryService;
    private final SseAlarmService sseAlarmService;

    // 주문 생성
    public Long createOrdering(List<OrderCreateDto> dtos) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        Member member = memberRepository.findByEmail(email).orElseThrow(() -> new EntityNotFoundException("없는 사용자입니다."));

        // 주문 먼저 저장하고
        Ordering ordering = Ordering.builder().orderStatus(OrderStatus.ORDERED).member(member).build();
        orderingRepository.save(ordering);

        // 저장한 주문번호 가져와서, OrderingDetail 저장
        // product id 뽑아서 product 객체 추출
        for(OrderCreateDto dto : dtos) {
            Product product = productRepository.findById(dto.getProductId()).orElseThrow(() -> new EntityNotFoundException("없는 상품입니다."));
            int quantity = dto.getProductCount();
            OrderDetail orderDetail = OrderDetail.builder().product(product).quantity(quantity).ordering(ordering).build();

            // @OneToMany + Cascade 조합으로 따로 save 없이 저장될 수 있게
            ordering.getOrderDetailList().add(orderDetail);

            // 재고 관리
            boolean check = product.decreaseQuantity(quantity); // 조건은 여기서 해결하는게 나을 듯
            if(!check) {
                // 예외 강제 발생시킴으로서, 모두 임시저장사항들을 Rollback처리
                // 모든 임시 저장 사항들을 롤백 처리
                throw new IllegalArgumentException("재고가 부족합니다.");
            }
//            1. 동시에 접근하는 상황에서 update갑의 정합성이 깨지고 갱신이상이 발생
//            2. spring버전이나 mysql버전에 따라 jpa에서 강제 에러(deadlock)을 유발시켜 대부분의 요청 실패 발생.
        }

        return ordering.getId();
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)//격리레벨을 낮춤으로써, 성능향상과 lock관련 문제 원천 차단.
    public Long createConcurrent(List<OrderCreateDto> dtos) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        Member member = memberRepository.findByEmail(email).orElseThrow(() -> new EntityNotFoundException("없는 사용자입니다."));

        // 주문 먼저 저장하고
        Ordering ordering = Ordering.builder().orderStatus(OrderStatus.ORDERED).member(member).build();
        orderingRepository.save(ordering);

        // 저장한 주문번호 가져와서, OrderingDetail 저장
        // product id 뽑아서 product 객체 추출
        for(OrderCreateDto dto : dtos) {
            Product product = productRepository.findById(dto.getProductId()).orElseThrow(() -> new EntityNotFoundException("없는 상품입니다."));
            int quantity = dto.getProductCount();
            OrderDetail orderDetail = OrderDetail.builder().product(product).quantity(quantity).ordering(ordering).build();

            // @OneToMany + Cascade 조합으로 따로 save 없이 저장될 수 있게
            ordering.getOrderDetailList().add(orderDetail);

//            redis에서 재고수량 확인 및 재고수량 감소처리
            int newQuantity = stockInventoryService.decreaseStockQuantity(product.getId(), dto.getProductCount());
            if(newQuantity < 0){
                throw new IllegalArgumentException("재고부족");
            }
        }

//        주문성공시 admin유저에게 메시지 전송
        sseAlarmService.publishMessage("admin@naver.com",email,ordering.getId());
        return ordering.getId();
    }

    // 주문 목록 조회
    public List<OrderListResDto> findAll() {
        List<Ordering> orderingList = orderingRepository.findAll();
        List<OrderListResDto> orderListResDtoList = new ArrayList<>();
        // Ordering (id, orderStatus), Member(memberEmail)
        // OrderDetail (detailId, productName, productCount)
        for(Ordering ordering : orderingList) {
            List<OrderDetail> orderDetailList = orderingDetailRepository.findByOrdering(ordering);
            List<OrderDetailDto> orderDetailDtoList = new ArrayList<>();
            for(OrderDetail orderDetail : orderDetailList) {
                orderDetailDtoList.add(OrderDetailDto.fromEntity(orderDetail));
            }
            OrderListResDto orderListResDto = OrderListResDto.fromEntity(ordering, orderDetailDtoList);
            orderListResDtoList.add(orderListResDto);
        }

        return orderListResDtoList;
    }
    public List<OrderListResDto> myorders() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        Member member = memberRepository.findByEmail(email).orElseThrow(()-> new EntityNotFoundException("member is not found"));

        List<Ordering> orderingList = orderingRepository.findAllByMember(member);
        List<OrderListResDto> orderListResDtoList = new ArrayList<>();

        // Ordering (id, orderStatus), Member(memberEmail)
        // OrderDetail (detailId, productName, productCount)
        for(Ordering ordering : orderingList) {
            List<OrderDetailDto> orderDetailDtoList = new ArrayList<>();

            List<OrderDetail> orderDetailList = orderingDetailRepository.findByOrdering(ordering);
            for(OrderDetail orderDetail : orderDetailList) {
                orderDetailDtoList.add(OrderDetailDto.fromEntity(orderDetail));
            }
            OrderListResDto orderListResDto = OrderListResDto.fromEntity(ordering, orderDetailDtoList);
            orderListResDtoList.add(orderListResDto);
        }

        return orderListResDtoList;
    }

    public void orderCancel(Long orderId) throws JsonProcessingException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();

//        Ordering DB에 상태값 변경 CANCELED
        Ordering ordering = orderingRepository.findById(orderId).orElseThrow(()->new EntityNotFoundException("주문을 찾을수없습니다."));
        ordering.cancelStatus(OrderStatus.CANCELED);
        for(OrderDetail orderDetail: ordering.getOrderDetailList()){
            orderDetail.getProduct().cancelOrder(orderDetail.getQuantity());
            stockInventoryService.increaseStockQuantity(
                    orderDetail.getProduct().getId(),
                    orderDetail.getQuantity()
            );
        }

    }
}
