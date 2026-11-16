package pt.utp.estg.pp.printhub.model;

public enum OrderStatus {
    PENDING_PAYMENT,   // Por pagar
    PAID,              // Paga
    PRINTING,          // Em impressão
    COMPLETED,         // Concluída
    SHIPPED,           // Enviada
    CANCELLED;         // Cancelada

    // Diz se é permitido passar deste estado para o "next"
    public boolean canChangeTo(OrderStatus next) {
        return switch (this) {
            case PENDING_PAYMENT -> next == PAID || next == CANCELLED;
            case PAID -> next == PRINTING || next == CANCELLED;
            case PRINTING -> next == COMPLETED;
            case COMPLETED -> next == SHIPPED;
            case SHIPPED, CANCELLED -> false;
        };
    }
}