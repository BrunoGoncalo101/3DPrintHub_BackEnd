package pt.utp.estg.pp.printhub.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import pt.utp.estg.pp.printhub.dto.CreateOrderRequest;
import pt.utp.estg.pp.printhub.dto.OrderLineResponse;
import pt.utp.estg.pp.printhub.dto.OrderResponse;
import pt.utp.estg.pp.printhub.dto.OrderStatusHistoryResponse;
import pt.utp.estg.pp.printhub.dto.UpdateOrderStatusRequest;
import pt.utp.estg.pp.printhub.entity.CartItem;
import pt.utp.estg.pp.printhub.entity.Order;
import pt.utp.estg.pp.printhub.entity.OrderLine;
import pt.utp.estg.pp.printhub.entity.OrderStatusHistory;
import pt.utp.estg.pp.printhub.entity.User;
import pt.utp.estg.pp.printhub.model.OrderStatus;
import pt.utp.estg.pp.printhub.repository.CartItemRepository;
import pt.utp.estg.pp.printhub.repository.OrderRepository;
import pt.utp.estg.pp.printhub.repository.OrderStatusHistoryRepository;
import pt.utp.estg.pp.printhub.repository.UserRepository;
import pt.utp.estg.pp.printhub.dto.AdminOrderSummaryResponse;
import java.time.LocalDate;
import java.time.LocalDateTime;
import pt.utp.estg.pp.printhub.dto.CustomerOrderSummaryResponse;
import pt.utp.estg.pp.printhub.dto.OrderDetailResponse;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final CartItemRepository cartItemRepository;
    private final UserRepository userRepository;
    private final OrderStatusHistoryRepository historyRepository;

    public OrderService(OrderRepository orderRepository,
                        CartItemRepository cartItemRepository,
                        UserRepository userRepository,
                        OrderStatusHistoryRepository historyRepository) {
        this.orderRepository = orderRepository;
        this.cartItemRepository = cartItemRepository;
        this.userRepository = userRepository;
        this.historyRepository = historyRepository;
    }

    // Cria uma encomenda com o que está no carrinho e depois esvazia o carrinho
    @Transactional
    public OrderResponse createFromCart(Integer userId, CreateOrderRequest request) {
        List<CartItem> cartItems = cartItemRepository.findByUserId(userId);
        if (cartItems.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O carrinho está vazio.");
        }

        Order order = new Order();
        order.setUser(userRepository.getReferenceById(userId));
        order.setShippingAddress(request.shippingAddress());

        BigDecimal total = BigDecimal.ZERO;

        for (CartItem item : cartItems) {
            if (!Boolean.TRUE.equals(item.getProduct().getActive())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "O produto " + item.getProduct().getName() + " já não está disponível.");
            }

            OrderLine line = new OrderLine();
            line.setProduct(item.getProduct());
            line.setMaterial(item.getMaterial());
            line.setColor(item.getColor());
            line.setQuantity(item.getQuantity());
            line.setUnitPrice(item.getProduct().getBasePrice());
            order.addLine(line);

            BigDecimal subtotal = line.getUnitPrice().multiply(BigDecimal.valueOf(line.getQuantity()));
            total = total.add(subtotal);
        }

        order.setTotal(total);
        Order saved = orderRepository.save(order);

        // Primeiro registo do histórico: a encomenda nasce "por pagar"
        saveHistory(saved, null, OrderStatus.PENDING_PAYMENT, saved.getUser());

        cartItemRepository.deleteAll(cartItems);

        return toResponse(saved);
    }

    // O admin muda o estado de uma encomenda (só se a transição for permitida)
    @Transactional
    public OrderResponse changeStatus(Integer orderId, Integer adminId, UpdateOrderStatusRequest request) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Encomenda não encontrada."));

        OrderStatus current = order.getStatus();
        OrderStatus next = request.status();

        if (!current.canChangeTo(next)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Não é possível passar de " + current + " para " + next + ".");
        }

        order.setStatus(next);
        orderRepository.save(order);

        saveHistory(order, current, next, userRepository.getReferenceById(adminId));

        return toResponse(order);
    }

    // Pagamento simulado: o cliente paga a sua encomenda (passa de PENDING_PAYMENT para PAID)
    @Transactional
    public OrderResponse pay(Integer userId, Integer orderId) {
        Order order = orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Encomenda não encontrada."));

        if (order.getStatus() != OrderStatus.PENDING_PAYMENT) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Esta encomenda já não está por pagar.");
        }

        order.setStatus(OrderStatus.PAID);
        orderRepository.save(order);

        saveHistory(order, OrderStatus.PENDING_PAYMENT, OrderStatus.PAID, order.getUser());

        return toResponse(order);
    }

    // Lista de encomendas para o admin, com filtros opcionais por estado e por datas
    @Transactional(readOnly = true)
    public List<AdminOrderSummaryResponse> searchForAdmin(OrderStatus status, LocalDate from, LocalDate to) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A data inicial não pode ser depois da data final.");
        }

        LocalDateTime fromTime = null;
        if (from != null) {
            fromTime = from.atStartOfDay();
        }

        LocalDateTime toTime = null;
        if (to != null) {
            toTime = to.plusDays(1).atStartOfDay();
        }

        List<AdminOrderSummaryResponse> result = new ArrayList<>();

        for (Order order : orderRepository.search(status, fromTime, toTime)) {
            result.add(new AdminOrderSummaryResponse(
                    order.getId(),
                    order.getUser().getName(),
                    order.getUser().getEmail(),
                    order.getStatus(),
                    order.getTotal(),
                    order.getCreatedAt(),
                    order.getLines().size()
            ));
        }

        return result;
    }

    // Lista as encomendas do cliente, da mais recente para a mais antiga
    @Transactional(readOnly = true)
    public List<CustomerOrderSummaryResponse> listMine(Integer userId) {
        List<CustomerOrderSummaryResponse> result = new ArrayList<>();

        for (Order order : orderRepository.findByUserIdOrderByCreatedAtDesc(userId)) {
            result.add(new CustomerOrderSummaryResponse(
                    order.getId(),
                    order.getStatus(),
                    order.getTotal(),
                    order.getCreatedAt(),
                    order.getLines().size()
            ));
        }

        return result;
    }

    // Detalhe de uma encomenda do cliente: linhas, total e histórico de estados
    @Transactional(readOnly = true)
    public OrderDetailResponse getMine(Integer userId, Integer orderId) {
        Order order = orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Encomenda não encontrada."));

        return new OrderDetailResponse(toResponse(order), getHistory(orderId));
    }

    // Devolve todas as mudanças de estado de uma encomenda, da mais antiga para a mais recente
    @Transactional(readOnly = true)
    public List<OrderStatusHistoryResponse> getHistory(Integer orderId) {
        if (!orderRepository.existsById(orderId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Encomenda não encontrada.");
        }

        List<OrderStatusHistoryResponse> result = new ArrayList<>();

        for (OrderStatusHistory h : historyRepository.findByOrderIdOrderByChangedAtAsc(orderId)) {
            String changedBy = null;
            if (h.getChangedBy() != null) {
                changedBy = h.getChangedBy().getName();
            }

            result.add(new OrderStatusHistoryResponse(
                    h.getPreviousStatus(),
                    h.getNewStatus(),
                    changedBy,
                    h.getChangedAt()
            ));
        }

        return result;
    }

    // Grava uma linha no histórico (usado ao criar e ao mudar o estado)
    private void saveHistory(Order order, OrderStatus previous, OrderStatus next, User changedBy) {
        OrderStatusHistory history = new OrderStatusHistory();
        history.setOrder(order);
        history.setPreviousStatus(previous);
        history.setNewStatus(next);
        history.setChangedBy(changedBy);
        historyRepository.save(history);
    }

    private OrderResponse toResponse(Order order) {
        List<OrderLineResponse> lines = new ArrayList<>();

        for (OrderLine line : order.getLines()) {
            BigDecimal subtotal = line.getUnitPrice().multiply(BigDecimal.valueOf(line.getQuantity()));
            lines.add(new OrderLineResponse(
                    line.getProduct().getId(),
                    line.getProduct().getName(),
                    line.getMaterial().getName(),
                    line.getColor().getName(),
                    line.getQuantity(),
                    line.getUnitPrice(),
                    subtotal
            ));
        }

        return new OrderResponse(
                order.getId(),
                order.getStatus(),
                order.getTotal(),
                order.getShippingAddress(),
                order.getCreatedAt(),
                lines
        );
    }
}