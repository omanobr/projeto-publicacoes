package com.pmba.publicacoes.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.hibernate.annotations.ColumnDefault;

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
    private String bgo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @ColumnDefault("'ATIVA'")
    private StatusPublicacao status;

    @Column(columnDefinition = "TEXT")
    private String conteudoHtml;

    @OneToMany(mappedBy = "publicacaoOrigem", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<VinculoNormativo> vinculosGerados = new HashSet<>();
}