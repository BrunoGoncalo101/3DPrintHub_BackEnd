package pt.utp.estg.pp.printhub.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "notificacoes")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "utilizador_id", nullable = false)
    private Integer userId;

    @Column(name = "titulo", nullable = false, length = 150)
    private String title;

    @Column(name = "mensagem", nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(name = "tipo", nullable = false, length = 40)
    private String type; // Ex: "ORDER_STATUS", "CUSTOM_REQUEST_STATUS"

    @Column(name = "referencia_id")
    private Integer referenceId;

    @Column(name = "lida", nullable = false)
    private Boolean read = false;

    @Column(name = "criada_em", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public Notification() {
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public Integer getReferenceId() { return referenceId; }
    public void setReferenceId(Integer referenceId) { this.referenceId = referenceId; }

    public Boolean getRead() { return read; }
    public void setRead(Boolean read) { this.read = read; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}