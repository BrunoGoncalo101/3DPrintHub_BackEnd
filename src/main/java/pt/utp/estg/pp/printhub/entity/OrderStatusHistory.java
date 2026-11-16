package pt.utp.estg.pp.printhub.entity;

import jakarta.persistence.*;
import pt.utp.estg.pp.printhub.model.OrderStatus;

import java.time.LocalDateTime;

@Entity
@Table(name = "historico_encomendas")
public class OrderStatusHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "encomenda_id", nullable = false)
    private Order order;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_anterior", length = 30)
    private OrderStatus previousStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_novo", nullable = false, length = 30)
    private OrderStatus newStatus;

    @ManyToOne
    @JoinColumn(name = "alterado_por")
    private User changedBy;

    @Column(name = "alterado_em", nullable = false)
    private LocalDateTime changedAt;

    public OrderStatusHistory() {
    }

    @PrePersist
    void onCreate() {
        this.changedAt = LocalDateTime.now();
    }

    public Integer getId() {
        return id;
    }

    public Order getOrder() {
        return order;
    }

    public void setOrder(Order order) {
        this.order = order;
    }

    public OrderStatus getPreviousStatus() {
        return previousStatus;
    }

    public void setPreviousStatus(OrderStatus previousStatus) {
        this.previousStatus = previousStatus;
    }

    public OrderStatus getNewStatus() {
        return newStatus;
    }

    public void setNewStatus(OrderStatus newStatus) {
        this.newStatus = newStatus;
    }

    public User getChangedBy() {
        return changedBy;
    }

    public void setChangedBy(User changedBy) {
        this.changedBy = changedBy;
    }

    public LocalDateTime getChangedAt() {
        return changedAt;
    }
}