package com.pmba.publicacoes.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.hibernate.annotations.ColumnDefault; // <-- IMPORTANTE

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

@Data
@EqualsAndHashCode(exclude = "vinculosGerados")
@ToString(exclude = "vinculosGerados")
@Entity
public class Publicacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String titulo;
    private String numero;
    private String tipo;
    private LocalDate dataPublicacao;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String conteudoHtml;

    // =================================================================
    // A SOLUÇÃO DEFINITIVA ESTÁ AQUI
    // =================================================================
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusPublicacao status = StatusPublicacao.ATIVA;
    // =================================================================

    @OneToMany(mappedBy = "publicacaoOrigem", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private Set<VinculoNormativo> vinculosGerados = new HashSet<>();
}